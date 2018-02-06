package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProcessRunner
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class AbstractDockerTask extends DefaultTask {

    File workingDir
    String dockerTag
    boolean dockerEnableVersion = true
    boolean dockerEnableBaseVersion = true
    boolean dockerEnableLatest = true

    def cmds

    @TaskAction
    def run() {
        initParams()
        execDockerCommands()
    }


    abstract def execDockerCommands()

    def initParams() {
        workingDir = workingDir ?: project.file('build')
        if (Env.config('dockerTag') != null) {
            dockerTag = Env.config('dockerTag')
        } else {
            dockerTag = "${Env.artifactId()}"
        }

        if (Env.config('dockerEnableVersion') != null) {
            dockerEnableVersion = Env.config('dockerEnableVersion').toBoolean()
        }


        if (Env.config('dockerEnableLatest') != null) {
            dockerEnableLatest = Env.config('dockerEnableLatest').toBoolean()
        }

        if (Env.config('dockerEnableBaseVersion') != null) {
            dockerEnableBaseVersion = Env.config('dockerEnableBaseVersion').toBoolean()
        }
    }

    def runDockerCmd(String dockerCmd) {
        new ProcessRunner(
                timeoutInMilliSeconds: Long.MAX_VALUE,
                dir: workingDir,
                cmds: [
                        "cd $workingDir.absolutePath",
                        dockerCmd
                ],
        ).execute()
    }
}
