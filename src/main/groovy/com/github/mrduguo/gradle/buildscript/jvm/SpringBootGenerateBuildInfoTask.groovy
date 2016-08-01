package com.github.mrduguo.gradle.buildscript.jvm

import com.github.mrduguo.gradle.buildscript.utils.Env
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SpringBootGenerateBuildInfoTask extends DefaultTask {

    @TaskAction
    def run() {
        def buildInfo=[]
        buildInfo << "info.build.version=${project.version}"
        buildInfo << "info.build.time=${new Date(java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime()).format('yyyy-MM-dd HH:mm:ss z')}"
        buildInfo << "info.build.user=${System.properties['user.name']}"
        def gitInfo=Env.gitInfo()
        if(gitInfo){
            buildInfo << "info.build.git.branch=${gitInfo.branch}"
            buildInfo << "info.build.git.commit=${gitInfo.commit}"
            buildInfo << "info.build.number=${gitInfo.buildNumber}"
        }
        buildInfo=buildInfo.join('\n')
        def outputFile=project.file('build/resources/main/application.properties')
        if(outputFile.exists()){
            outputFile.append("\n$buildInfo")
        }else{
            outputFile.parentFile.mkdirs()
            outputFile << buildInfo
        }
    }
}
