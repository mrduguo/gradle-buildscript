package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProcessRunner
import com.github.mrduguo.gradle.buildscript.utils.TextHelper
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/** USAGE:

 ./gradlew -Dtest.single=BldUpgradeTaskTest -DgradleVersion=latest -DtargetProjectRoot=build/test-root

 ./gradelw bld_upgrade

 */

class BldUpgradeTask extends DefaultTask {

    @TaskAction
    def run() {
        def simplestProjectDir = new File(project.buildDir, 'gradle_simplest_project')
        copyGradleFiles(project.projectDir, simplestProjectDir)
        prepareGradleWrapperTask(simplestProjectDir)
        runGradleWrapperAndApplyFixes(simplestProjectDir)
        runGradleWrapperAndApplyFixes(simplestProjectDir)

        def targetProjectRoot = new File(Env.config('targetProjectRoot', project.projectDir.absolutePath))
        copyGradleFiles(simplestProjectDir, targetProjectRoot)
        copyResourceFile(simplestProjectDir, targetProjectRoot, '/com/github/mrduguo/gradle/buildscript/simplest-project-build.gradle', 'build.gradle')
        copyResourceFile(simplestProjectDir, targetProjectRoot, '/buildscript.gitignore', '.gitignore')
        println "upgraded project at $targetProjectRoot.absolutePath"
    }

    def copyResourceFile(File simplestProjectDir, File targetProjectRoot, String sourcePath, String targetPath) {
        def resource = getClass().getResource(sourcePath)
        if (!resource) {
            resource = new File("../../../build/resources/main$sourcePath")
        }
        new File(simplestProjectDir, targetPath).write(resource.text)

        if (targetPath == 'build.gradle') {
            def buildGradleFile = new File(targetProjectRoot, targetPath)
            if (buildGradleFile.exists()) {
                if (!buildGradleFile.text.contains('/com/github/mrduguo/gradle/buildscript/buildscript.gradle')) {
                    def dependenciesGradleFile = new File(targetProjectRoot, 'gradle/dependencies.gradle')
                    dependenciesGradleFile.write(buildGradleFile.text)
                }
            }
            buildGradleFile.write(resource.text)
        } else {
            new File(targetProjectRoot, targetPath).write(resource.text)
        }
    }

    def prepareGradleWrapperTask(File simplestProjectDir) {
        String gradleVersion = detectGradleVersion()
        def gradleBuildFile = new File(simplestProjectDir, 'build.gradle')
        gradleBuildFile.write("""
defaultTasks 'wrapper'
task wrapper(type: Wrapper) {
   gradleVersion = '$gradleVersion'
}
""")
    }

    def copyGradleFiles(File from, File to) {
        FileUtils.copyDirectory(new File(from, 'gradle/wrapper'), new File(to, 'gradle/wrapper'))
        Files.copy(Paths.get(new File(from, 'gradlew').toURI()), Paths.get(new File(to, 'gradlew').toURI()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        if (new File(from, 'gradlew.bat').exists() && new File(to, 'gradlew.bat').exists()) {
            Files.copy(Paths.get(new File(from, 'gradlew.bat').toURI()), Paths.get(new File(to, 'gradlew.bat').toURI()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    def detectGradleVersion() {
        def gradleVersion = Env.config('gradleVersion')
        if (gradleVersion) {
            if (gradleVersion == 'latest') {
                def gradleLatestVersionInfo = new JsonSlurper().parse(new URL('https://services.gradle.org/versions/current'))
                gradleVersion = gradleLatestVersionInfo.version
                println "detected latest gradle version as: $gradleVersion"
            }
        } else {
            getClass().getResourceAsStream('/buildscript-build-info.properties').eachLine { String line ->
                def lineInfo = line.split('=', 2)
                if (lineInfo[0] == 'GRADLE_VERSION') {
                    gradleVersion = lineInfo[1]
                }
            }
        }
        println "use gradle version $gradleVersion"
        gradleVersion
    }

    def runGradleWrapperAndApplyFixes(File simplestProjectDir) {
        new ProcessRunner(dir: simplestProjectDir, cmds: ['./gradlew']).execute()
        applyFixes(simplestProjectDir)
    }

    def applyFixes(File simplestProjectDir) {
        TextHelper.replaceText(new File(simplestProjectDir, 'gradle/wrapper/gradle-wrapper.properties'), '-bin.zip', '-all.zip')
        TextHelper.replaceText(new File(simplestProjectDir, 'gradlew'), 'GradleWrapperMain "$@"', 'GradleWrapperMain -s --no-daemon "$@"')
        TextHelper.replaceText(new File(simplestProjectDir, 'gradlew.bat'), 'GradleWrapperMain %CMD_LINE_ARGS%', 'GradleWrapperMain -s --no-daemon %CMD_LINE_ARGS%')
    }
}
