package com.github.mrduguo.gradle.buildscript.bld

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class BldStatsTask extends DefaultTask {

    @TaskAction
    def run() {
        println "\n\n\nSystem Properties:"
        failSafe({
            System.properties.sort().each { k, v ->
                println "$k = $v"
            }
        })

        println "\n\n\nEnvrionment Variables:"
        failSafe({
            System.getenv().sort().each { k, v ->
                println "$k = $v"
            }
        })

        println "\n\n\nGit Stats:"
        execGitCommand 'git rev-parse --abbrev-ref HEAD'
        execGitCommand 'git rev-parse --short HEAD'

        println "\n\n\nProject Ext:"
        project.getExtensions().extraProperties.properties.sort().each { k, v ->
            println "$k = $v"
        }
        println "project.version = $project.version"
    }

    def failSafe(Closure action) {
        try {
            action()
        } catch (Throwable ex) {
            println "ignore execute task failed ${ex.getClass().name}: $ex.message"
            ex.printStackTrace()
        }
    }

    def execGitCommand(String cmd) {
        println "executing... : $cmd"
        failSafe({
            println cmd.execute().text.trim()
        })
    }


}
