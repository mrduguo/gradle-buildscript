package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.dist.DistPlugin
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PublishInitScriptTask extends DefaultTask {

    @TaskAction
    def run() {
        publishFile(project.file('build/resources/main/com/github/mrduguo/gradle/buildscript/buildscript.gradle'), 'com/github/mrduguo/gradle/buildscript/buildscript.gradle')
        println """Usage:

        ./gradlew -DmavenRepoUrl=$project.ext.mavenRepoUrl

        ./gradlew -DmavenRepoUrl=$project.ext.mavenRepoUrl -DbuildscriptVersion=$project.version

"""
    }


    def publishFile(file, path) {
        if (project.ext.mavenRepoUrl.startsWith('https://s3')) {
            def s3client = new com.amazonaws.services.s3.AmazonS3Client()
            def urlInfo = project.ext.mavenRepoUrl.split('/')
            def bucketName = urlInfo[3]
            def keyName = "${urlInfo[4..-1].join('/')}/$path"
            s3client.putObject(new com.amazonaws.services.s3.model.PutObjectRequest(bucketName, keyName, file))
            println "Upload s3://$bucketName/$keyName"
        } else if (DistPlugin.isBintrayRepoEnabled()) {
            if(ProjectHelper.isTaskExist('recordingPublishInitScriptTask')){
                println "Uploaded to $project.ext.mavenRepoUrl$path"
            }
        } else if (project.ext.mavenRepoUrl.startsWith('file:')) {
            def targetFile = new File(project.ext.mavenRepoUrl.substring(7), path)
            targetFile.parentFile.mkdirs()
            targetFile.bytes = file.bytes
            println "copied to $targetFile.absolutePath"
        }
    }

}
