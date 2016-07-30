package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper

class DockerBuildTask extends AbstractDockerTask {

    def execDockerCommands() {
        def versionedTag="$dockerTag:${ProjectHelper.project.version}"
        runDockerCmd("docker build -t $versionedTag .")
        if(dockerLatest){
            def latestTag="$dockerTag:latest"
            runDockerCmd("docker tag $versionedTag $latestTag")
        }
    }

}
