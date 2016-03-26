package com.github.mrduguo.gradle.buildscript.jvm

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class FakeBootRunTask extends DefaultTask {

    @TaskAction
    def run() {
        // nothing as work will be done in bootRun
    }

    String getMain() {
        null
    }
}
