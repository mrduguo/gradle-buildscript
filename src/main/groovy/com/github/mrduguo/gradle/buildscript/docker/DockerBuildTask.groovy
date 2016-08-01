package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper

class DockerBuildTask extends AbstractDockerTask {

    def execDockerCommands() {
        def versionedTag="$dockerTag:${ProjectHelper.project.version}"
        runDockerCmd("docker build -t $versionedTag .")
        if(dockerEnableBaseVersion){
            def baseVersionTag="$dockerTag:${Env.config('baseVersion')}"
            runDockerCmd("docker tag $versionedTag $baseVersionTag")
        }
        if(dockerEnableLatest){
            def latestTag="$dockerTag:latest"
            runDockerCmd("docker tag $versionedTag $latestTag")
        }
    }

}
