package com.github.mrduguo.gradle.buildscript.docker

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



            def dockerTask = project.getTasks().create('docker', DockerTask.class)
            dockerTask.workingDir = project.file('build')
            project.getTasks().getByName('assemble').dependsOn(dockerTask)

            def processResourceTask = project.getTasks().create('dockerResources', ProcessResources.class)
            processResourceTask.from(project.file('src/main/docker'))
            processResourceTask.expand(project.properties)
            processResourceTask.destinationDir(project.file('build'))
            dockerTask.dependsOn(processResourceTask)

            ProjectHelper.doIfTaskExist('test') { def testTask ->
                dockerTask.dependsOn(testTask)
            }

            if(ProjectHelper.isTaskExist('bootRepackage')){
                processResourceTask.dependsOn(project.getTasks().getByName('bootRepackage'))
            }else if(ProjectHelper.isTaskExist('jar')){
                processResourceTask.dependsOn(project.getTasks().getByName('jar'))
            }
        }
    }
}