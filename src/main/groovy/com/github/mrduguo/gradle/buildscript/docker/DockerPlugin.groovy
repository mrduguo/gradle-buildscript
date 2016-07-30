package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.dist.DistPlugin
import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.jvm.tasks.ProcessResources

class DockerPlugin implements Plugin<Project> {

    public static boolean IS_DOCKER_PROJECT = false

    @Override
    void apply(Project project) {
        IS_DOCKER_PROJECT = project.file('src/main/docker/Dockerfile').exists()
        if (IS_DOCKER_PROJECT) {
            if (project.getTasksByName('assemble', true).empty) {
                project.getPlugins().apply(BasePlugin)
            }

            def dockerBuildTask = setupBuildTask(project)
            setupResourceTask(project,dockerBuildTask)
            setupPushTask(project, dockerBuildTask)
        }
    }

    def setupBuildTask(Project project) {
        def dockerBuildTask = project.getTasks().create('dockerBuild', DockerBuildTask.class)
        dockerBuildTask.workingDir = project.file('build')
        project.getTasks().getByName('assemble').dependsOn(dockerBuildTask)
        dockerBuildTask
    }

    void setupResourceTask(Project project,def dockerBuildTask) {
        def processResourceTask = project.getTasks().create('dockerResources', ProcessResources.class)
        processResourceTask.from(project.file('src/main/docker'))
        processResourceTask.expand(project.properties)
        processResourceTask.destinationDir(project.file('build'))
        dockerBuildTask.dependsOn(processResourceTask)

        ProjectHelper.doIfTaskExist('test') { def testTask ->
            dockerBuildTask.dependsOn(testTask)
        }

        if (ProjectHelper.isTaskExist('bootRepackage')) {
            processResourceTask.dependsOn(project.getTasks().getByName('bootRepackage'))
        } else if (ProjectHelper.isTaskExist('jar')) {
            processResourceTask.dependsOn(project.getTasks().getByName('jar'))
        }
    }

    void setupPushTask(Project project, def dockerBuildTask) {
        def dockerPush
        if (Env.config('dockerPush')) {
            dockerPush = Env.config('dockerPush').toBoolean()
        } else {
            dockerPush = Env.jobName() ? true : false
        }

        if (dockerPush) {
            def dockerPushTask = project.getTasks().create('dockerPush', DockerPushTask.class)
            dockerPushTask.dependsOn(dockerBuildTask)
            project.afterEvaluate {
                if (ProjectHelper.isTaskExist('check')) {
                    dockerPushTask.dependsOn ProjectHelper.getTask('check')
                }
                ProjectHelper.getTask('build').dependsOn dockerPushTask
            }
        }
    }
}