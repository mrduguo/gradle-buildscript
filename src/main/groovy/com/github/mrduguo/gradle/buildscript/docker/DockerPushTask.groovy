package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper

class DockerPushTask extends AbstractDockerTask {

    def execDockerCommands() {
        if(dockerEnableVersion){
            def versionedTag = "$dockerTag:${ProjectHelper.project.version}"
            runDockerCmd("docker push $versionedTag")
        }
        if(dockerEnableBaseVersion){
            def baseVersionTag="$dockerTag:${Env.config('baseVersion')}"
            runDockerCmd("docker push $baseVersionTag")
        }
        if (dockerEnableLatest) {
            def latestTag = "$dockerTag:latest"
            runDockerCmd("docker push $latestTag")
        }
    }

}
