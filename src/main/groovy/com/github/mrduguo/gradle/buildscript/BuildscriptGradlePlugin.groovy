package com.github.mrduguo.gradle.buildscript

import com.github.mrduguo.gradle.buildscript.bld.BldProjectPlugin
import com.github.mrduguo.gradle.buildscript.dist.DistPlugin
import com.github.mrduguo.gradle.buildscript.docker.DockerPlugin
import com.github.mrduguo.gradle.buildscript.jvm.JvmPlugin
import com.github.mrduguo.gradle.buildscript.nodejs.NodejsPlugin
import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.Logger
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

class BuildscriptGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        ProjectHelper.project = project
        project.plugins.apply(BldProjectPlugin)
        applyScripts(project, '00-')
        if (Env.config('buildscriptDisableAutoConfig', 'false') == 'false') {
            initEnvironment(project)
            activeProjectLanguageSupport(project)
            registerTasks(project)
        }
        applyScripts(project, '!00-')
    }

    def initEnvironment(Project project) {
        Properties pro = new Properties()
        pro.load(getClass().getResourceAsStream('/com/github/mrduguo/gradle/buildscript/buildscript.properties'))
        pro.each { k, v ->
            if(!project.hasProperty(k)){
                project.ext.set(k, v)
            }
        }
        def baseVersion
        if(project.hasProperty('baseVersion')){
            baseVersion=project.baseVersion
        }else{
            baseVersion=project.ext.libBuildscriptVersion.split('-').first()
        }
        project.version = "${baseVersion}-${new Date(java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime()).format('yyMMdd-HHmmss')}".toString()
        if(Env.isGitProject()){
            project.version = "${project.version}-${Env.config('GIT_COMMIT')?.substring(0, 7) ?: 'git rev-parse --short HEAD'.execute().text.trim()}-${Env.config('BUILD_NUMBER') ?: Env.config('TRAVIS_BUILD_NUMBER', '0')}".toString()
        }
        println "${new Date().format('yy-MM-dd HH:mm:ss')}  kick off build with buildscript version $project.ext.libBuildscriptVersion"
    }

    def activeProjectLanguageSupport(Project project) {
        project.plugins.apply(DistPlugin)
        project.plugins.apply(JvmPlugin)
        project.plugins.apply(NodejsPlugin)
        project.plugins.apply(DockerPlugin)
    }


    def registerTasks(Project project) {
        project.defaultTasks('clean', 'build')

        def taskNames = collectDefaultTaskNames(project)
        if (taskNames) {
            tryToRegisterTasks(project, taskNames, false)
        } else {
            def jobName = Env.config('JOB_NAME')
            if (jobName) {
                taskNames = jobName.split('-')
                tryToRegisterTasks(project, taskNames, true)
            }
        }

        if (project.getTasksByName('clean', true).empty) {
            project.getPlugins().apply(BasePlugin)
        }
    }


    def tryToRegisterTasks(Project project, def taskNames, def setupDefaultTasks) {
        def foundTasks = []
        taskNames.each { String taskName ->
            if (project.getTasksByName(taskName, true).empty) {
                try {
                    def taskClass = Class.forName("com.github.mrduguo.gradle.buildscript.${taskName.split('_').first()}.${taskName.split('_').collect { it.capitalize() }.join()}Task")
                    project.getTasks().create(taskName, taskClass)
                    Logger.debug("created task: $taskName")
                    foundTasks << taskName
                } catch (ClassNotFoundException ignoreNoTaskFound) {
                }
            }
        }
        if (foundTasks && setupDefaultTasks) {
            def gradleDefaultTasks = new ArrayList<String>()
            gradleDefaultTasks.add('clean')
            gradleDefaultTasks.addAll(foundTasks)
            project.setDefaultTasks(gradleDefaultTasks)
            Logger.info("set default tasks: ${gradleDefaultTasks}")
        }
    }

    def collectDefaultTaskNames(Project project) {
        def taskNames = []
        project.gradle.startParameter?.taskNames.each { String taskName ->
            Logger.debug("found command line based task candidate: $taskName")
            taskNames << taskName
        }
        taskNames
    }

    def applyScripts(Project project, String prefix) {
        def isReversedExclude = prefix.startsWith('!')
        if (isReversedExclude) {
            prefix = prefix.substring(1)
        }
        project.file('gradle').list()?.findAll { it.endsWith('.gradle') }.sort().each { def fileName ->
            if (
            (fileName.startsWith(prefix) && !isReversedExclude) ||
                    (!fileName.startsWith(prefix) && isReversedExclude)
            ) {
                project.apply from: project.file("gradle/$fileName")
            }
        }
    }
}