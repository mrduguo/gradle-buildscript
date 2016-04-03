package com.github.mrduguo.gradle.buildscript.jvm

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.listener.ClosureBackedMethodInvocationDispatch
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.springframework.boot.gradle.SpringBootPlugin

import java.util.regex.Pattern

class JvmPlugin implements Plugin<Project> {

    public static boolean IS_SPRING_BOOT_PROJECT = false
    public static boolean IS_SPRING_FRAMEWORK_PROJECT = false
    public static boolean IS_GROOVY_PROJECT = false

    @Override
    void apply(Project project) {
        detectProjectType(project)

        if (IS_GROOVY_PROJECT) {
            project.getPlugins().apply(GroovyPlugin)
            setupLibVersions(project)
            project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME).setArchiveName(Env.artifactId() + "-" + project.version + ".jar")

            if (IS_SPRING_BOOT_PROJECT) {
                project.getPlugins().apply(SpringBootPlugin)
            }
        }

        ProjectHelper.doIfTaskExist('test') { def testTask ->
            if (testTask instanceof Test) {
                configTest(project)
                configAdditionalScope(project, IS_SPRING_BOOT_PROJECT ? 'provided' : 'optional')

                if (!IS_SPRING_BOOT_PROJECT) {
                    ProjectHelper.doIfTaskExist('jar') {
                        setupSourceJarTasks(project)
                    }
                }

                ProjectHelper.doIfTaskExist('jar') {def jarTask->
                    jarTask.setArchiveName(Env.artifactId() + "-" + project.version + ".jar")
                }
                project.sourceCompatibility = '1.7'
                project.targetCompatibility = '1.7'

                //TODO to verify in gradle-sample-app project
                project.configurations.compile.resolutionStrategy.cacheDynamicVersionsFor 0, 'seconds'
                project.configurations.testCompile.resolutionStrategy.cacheDynamicVersionsFor 0, 'seconds'
            }
        }

        project.gradle.startParameter?.taskNames.each {
            if (it == 'run') {
                def fakeBootRunTask = project.getTasks().create('run', FakeBootRunTask.class)
                fakeBootRunTask.dependsOn(ProjectHelper.getTask('bootRun'))
            }
        }
    }


    def setupLibVersions(Project project) {
        project.buildscript.configurations.classpath.each { File classpath ->
            extractLibVersion(project, classpath, 'SpringBoot', 'spring-boot-gradle-plugin')
            extractLibVersion(project, classpath, 'SpringFramework', 'spring-core')
        }
        new File(project.gradle.startParameter.gradleHomeDir, 'lib').listFiles().each { File classpath ->
            extractLibVersion(project, classpath, 'Groovy', 'groovy-all')
        }
    }

    def setupSourceJarTasks(Project project) {
        Jar sourcesJarTask = project.getTasks().create("sourcesJar", Jar.class)
        sourcesJarTask.classifier = 'sources'
        sourcesJarTask.setArchiveName(Env.artifactId() + "-" + project.version + "-sources.jar")
        project.afterEvaluate {
            project.sourceSets.main.java.source.each {
                sourcesJarTask.from(it)
            }
            project.sourceSets.main.groovy.source.each {
                sourcesJarTask.from(it)
            }
            project.getTasks().getByName('assemble').dependsOn(sourcesJarTask)
        }
    }

    def extractLibVersion(Project project, File classpath, String libName, String libArtifact) {
        def matcher = Pattern.compile("$libArtifact-(.*)\\.jar").matcher(classpath.name)
        if (matcher.find()) {
            project.ext["lib${libName}Version".toString()] = matcher[0][1]
        }
    }

    def detectProjectType(Project project) {
        project.file('gradle').listFiles().each { File gradleFile ->
            if (gradleFile.isFile()) {
                def fileText = gradleFile.text
                IS_SPRING_BOOT_PROJECT = IS_SPRING_BOOT_PROJECT ?: (fileText.contains('libSpringBootVersion') && project.file('src/main').exists())
                IS_SPRING_FRAMEWORK_PROJECT = IS_SPRING_FRAMEWORK_PROJECT ?: fileText.contains('libSpringFrameworkVersion')
                IS_GROOVY_PROJECT = IS_GROOVY_PROJECT ?: fileText.contains('libGroovyVersion')
            }
        }
        if (IS_SPRING_BOOT_PROJECT) {
            IS_SPRING_FRAMEWORK_PROJECT = true
        }
        if (IS_SPRING_FRAMEWORK_PROJECT) {
            IS_GROOVY_PROJECT = true
        }
    }

    def configTest(Project project) {
        project.afterEvaluate(new Action<Project>() {
            public void execute(Project p) {
                p.getTasks().withType(Test.class, new Action<Test>() {
                    public void execute(Test test) {
                        test.systemProperties = System.properties
                        test.testLogging.showStandardStreams = true
                        test.testLogging.exceptionFormat = 'full'
                        test.testListenerBroadcaster.add(new ClosureBackedMethodInvocationDispatch("beforeTest", { descriptor ->
                            project.logger.lifecycle("Running $descriptor")
                        }))
                    }
                })
            }
        })
    }

    def configAdditionalScope(Project project, def scope) {
        def scopeConfig = project.configurations.create(scope)
        project.sourceSets.main.compileClasspath += scopeConfig
        project.sourceSets.test.compileClasspath += scopeConfig
        project.sourceSets.test.runtimeClasspath += scopeConfig
        project.plugins.withType(IdeaPlugin) {
            project.idea.module {
                scopes.COMPILE.plus += [scopeConfig]
            }
        }
        project.plugins.withType(EclipsePlugin) {
            project.eclipse.classpath.plusConfigurations += [scopeConfig]
        }
    }

}