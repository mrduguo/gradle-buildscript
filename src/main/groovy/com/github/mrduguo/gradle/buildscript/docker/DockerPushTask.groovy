package com.github.mrduguo.gradle.buildscript.docker

import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper

class DockerPushTask extends AbstractDockerTask {

    def execDockerCommands() {
        def versionedTag = "$dockerTag:${ProjectHelper.project.version}"
        runDockerCmd("docker push $versionedTag")
        if (dockerLatest) {
            def latestTag = "$dockerTag:latest"
            runDockerCmd("docker push $latestTag")
        }
    }

}
