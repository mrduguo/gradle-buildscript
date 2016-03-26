package com.github.mrduguo.gradle.buildscript.bld

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GenerateBuildInfoTask extends DefaultTask {

    @TaskAction
    def run() {
        new File(project.buildDir, 'resources/main/buildscript-build-info.properties').write("""\
ARTIFACT_NAME=buildscript
ARTIFACT_VERSION=$project.version
GIT_BRANCH=$project.ext.gitBranch
GIT_COMMIT=$project.ext.gitCommit
GRADLE_VERSION=${project.file('gradle/wrapper/gradle-wrapper.properties').text.split('-')[-2]}
MAVEN_REPO_URL=$project.ext.mavenRepoUrl
BUILD_TIMESTAMP=${new Date()}
BUILD_URL=${System.getenv().BUILD_URL}
BUILD_USER_ID=${System.getenv().BUILD_USER_ID ?: System.getenv().BUILD_USER ?: System.getProperty('user.name')}
BUILD_SCRIPT_VERSION=$project.version\
""")
        new File(project.buildDir, 'resources/main/buildscript.gitignore').write(project.file('.gitignore').text)
    }


}
