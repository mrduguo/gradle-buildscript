package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProcessRunner
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class DockerTask extends DefaultTask {

    File workingDir
    String dockerTag
    boolean dockerPush = false
    boolean dockerLatest = true

    def cmds

    @TaskAction
    def run() {
        initParams()
        execDockerCommands()
    }


    def execDockerCommands() {
        def versionedTag="$dockerTag:${ProjectHelper.project.version}"
        runDockerCmd("docker build -t $versionedTag .")
        if(dockerPush){
            runDockerCmd("docker push $versionedTag")
        }
        if(dockerLatest){
            def latestTag="$dockerTag:latest"
            runDockerCmd("docker tag $versionedTag $latestTag")
            if(dockerPush){
                runDockerCmd("docker push $latestTag")
            }
        }
    }

    def initParams() {
        if (Env.config('dockerTag') != null) {
            dockerTag = Env.config('dockerTag')
        } else {
            dockerTag = "${Env.artifactId()}"
        }

        if (Env.config('dockerPush') != null) {
            dockerPush = Env.config('dockerPush').toBoolean()
        } else {
            dockerPush = Env.jobName() ? true : false
        }

        if (Env.config('dockerLatest') != null) {
            dockerLatest = Env.config('dockerLatest').toBoolean()
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
