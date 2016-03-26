package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin

class BldProjectPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (isBuildscriptProject(project)) {
            project.plugins.apply(GroovyPlugin)
            def generateBuildInfoTask = project.getTasks().create('generateBuildInfo', GenerateBuildInfoTask.class)
            generateBuildInfoTask.dependsOn ProjectHelper.getTask('processResources')
            ProjectHelper.getTask('jar').dependsOn generateBuildInfoTask
            def publishInitScript = project.getTasks().create('publishInitScript', PublishInitScriptTask.class)


            project.ext.mavenRepoUrl = System.properties.mavenRepoUrl ?: (System.properties.JOB_NAME ?: System.getenv().JOB_NAME) ? 'https://s3-eu-west-1.amazonaws.com/elasticbeanstalk-eu-west-1-349318639323/maven-repo/' : "file://$project.rootProject.projectDir/repo/".toString()

            project.ext.gitBranch = System.properties.GIT_BRANCH ?: System.getenv().GIT_BRANCH ?: 'git rev-parse --abbrev-ref HEAD'.execute().text.trim()
            project.ext.gitCommit = System.getenv().GIT_COMMIT?.substring(0, 7) ?: 'git rev-parse --short HEAD'.execute().text.trim()

            if (System.properties.projectBaseVersion) {
                project.ext.projectBaseVersion = System.properties.projectBaseVersion
            } else {
                def branchVersionMatcher = (project.ext.gitBranch =~ /\/((\d+\.\d+\.)(\d+))/)
                if (branchVersionMatcher.find()) {
                    branchVersionMatcher = branchVersionMatcher[0]
                    project.ext.projectBaseVersion = branchVersionMatcher[1]
                } else {
                    project.ext.projectBaseVersion = project.ext.baseVersion
                }
            }

            project.version = "$project.ext.projectBaseVersion-${new Date().format('yyMMdd-HHmmss')}-$project.ext.gitCommit-${System.getenv().BUILD_NUMBER ?: '0'}".toString()
            println "libBuildscriptVersion=$project.version"
        }
    }

    def isBuildscriptProject(Project project) {
        project.file('buildSrc/src/main/resources/META-INF/gradle-plugins/com.github.mrduguo.gradle.buildscript.properties').exists()
    }

}