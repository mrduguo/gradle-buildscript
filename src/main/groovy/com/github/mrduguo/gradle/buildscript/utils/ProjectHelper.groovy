package com.github.mrduguo.gradle.buildscript.utils

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task

class ProjectHelper {

    public static Project project

    static def boolean doIfPathsExists(List pathsToCheck, Closure action) {
        if (isAnyPathExist(pathsToCheck)) {
            action()
        }
    }

    static def boolean doIfTaskExist(String taskName, Closure action) {
        def tasks = project.getTasksByName(taskName, false)
        if (!tasks.empty) {
            action(tasks.first())
        }
    }

    static def Task getTask(String taskName) {
        def tasks = project.getTasksByName(taskName, false)
        if (!tasks.empty) {
            tasks.first()
        }
    }

    static def boolean doIfTaskExecuted(String taskName, Closure action) {
        doIfTaskExist(taskName) { Task task ->
            if (task.state.executed) {
                action(task)
            }
        }
    }

    static def boolean isTaskExecuted(String taskName) {
        getTask(taskName)?.state.executed
    }

    static def boolean isTaskExist(String taskName) {
        !project.getTasksByName(taskName, false).empty
    }

    static def Task safeGetRunTask() {
        if (!isTaskExist('run')) {
            project.getTasks().create('run', DefaultTask.class)
        }
        getTask('run')
    }


    static def boolean isAnyPathExist(List pathsToCheck) {
        for (String path in pathsToCheck) {
            if (project.file(path).exists()) {
                return true
            }
        }
        false
    }
}
