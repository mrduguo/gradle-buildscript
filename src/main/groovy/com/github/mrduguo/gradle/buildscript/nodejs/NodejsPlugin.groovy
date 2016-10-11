package com.github.mrduguo.gradle.buildscript.nodejs

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

class NodejsPlugin implements Plugin<Project> {

    public static boolean IS_NPM_PROJECT = false

    @Override
    void apply(Project project) {
        def packageJsonFile = detectPackageJson(project)
        if (packageJsonFile) {
            IS_NPM_PROJECT = true
            def taskName = 'npm'
            def npmRunCmd = null
            def runInBackground = false
            project.gradle.startParameter?.taskNames.each {
                if (it.startsWith('npm_run')) {
                    taskName = it
                    if (it.startsWith('npm_run_')) {
                        npmRunCmd = it.split('_', 3).last()
                    } else {
                        npmRunCmd = 'start'
                    }
                } else if (it == 'run') {
                    npmRunCmd = 'start'
                    if (ProjectHelper.isTaskExist('run')) {
                        runInBackground = true
                    }
                }
            }
            NpmTask npmTask = project.getTasks().create(taskName, NpmTask.class)
            npmTask.workingDir = packageJsonFile.parentFile
            npmTask.npmRunCmd = npmRunCmd
            npmTask.runInBackground = runInBackground
            if (ProjectHelper.isTaskExist('processResources')) {
                project.getTasks().getByName('processResources').dependsOn(npmTask)
            } else {
                if (project.getTasksByName('assemble', true).empty) {
                    project.getPlugins().apply(BasePlugin)
                }
                project.getTasks().getByName('assemble').dependsOn(npmTask)
            }

            if (npmRunCmd == 'start') {
                ProjectHelper.safeGetRunTask().dependsOn(npmTask)
            }

            if (Env.config('cleanNpmNodeModules', 'false') == 'true') {
                npmTask.outputFolders().each {
                    project.getTasks().getByName('clean').delete(it)
                }
            }

        }
    }


    File detectPackageJson(Project project) {
        def packageJsonFile = project.file('package.json')
        if (!packageJsonFile.exists()) {
            packageJsonFile = project.file('src/main/webapp/package.json')
        }
        if (packageJsonFile.exists()) {
            packageJsonFile
        } else {
            null
        }
    }

}