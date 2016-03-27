package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.dist.DistPlugin
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import com.jfrog.bintray.gradle.RecordingCopyTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.Copy

class BldProjectPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (isBuildscriptProject(project)) {
            project.plugins.apply(GroovyPlugin)
            def publishInitScript = project.getTasks().create('publishInitScript', PublishInitScriptTask.class)
            project.afterEvaluate {
                ProjectHelper.getTask('build').dependsOn publishInitScript
                if(DistPlugin.isBintrayRepoEnabled()){
                    def bintrayUpload=ProjectHelper.getTask('bintrayUpload')
                    publishInitScriptToBintray(project,bintrayUpload)
                    publishInitScript.dependsOn bintrayUpload
                }else{
                    publishInitScript.dependsOn ProjectHelper.getTask('publish')
                }
            }
        }
    }

    def publishInitScriptToBintray(Project project,Task bintrayUpload) {
        try{
            new URL("${project.ext.mavenRepoUrl}com/github/mrduguo/gradle/buildscript/buildscript.gradle").text
        }catch (Exception ex){
            Copy recordingPublishInitScriptTask = project.getTasks().create('recordingPublishInitScriptTask', RecordingCopyTask.class)
            recordingPublishInitScriptTask.from(project.file('build/resources/main/com/github/mrduguo/gradle/buildscript/buildscript.gradle'))
            recordingPublishInitScriptTask.into('com/github/mrduguo/gradle/buildscript/')
            recordingPublishInitScriptTask.outputs.upToDateWhen {
                false
            }
            bintrayUpload.dependsOn recordingPublishInitScriptTask

        }
    }

    def isBuildscriptProject(Project project) {
        project.file('src/main/resources/META-INF/gradle-plugins/com.github.mrduguo.gradle.buildscript.properties').exists()
    }

}