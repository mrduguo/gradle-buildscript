package com.github.mrduguo.gradle.buildscript.utils

import java.nio.file.Paths

class Env {

    public static String config(String key, String defaultValue = null) {
        System.properties[key] ?: System.getenv(key) ?: defaultValue
    }

    public static String jobName(String defaultName = null) {
        config('JOB_NAME', defaultName)
    }

    public static String artifactId() {
        def name = config('artifactId')
        if (!name) {
            def jenkinsJobName = jobName()
            if (jenkinsJobName && jenkinsJobName.contains('-')) {
                name = jobName().split('-')[1]
            } else {
                name = new File(Paths.get('').toFile().absolutePath).name
            }
        }
        name
    }


    public static void doIfHasConfig(def configName, def action) {
        def value = config(configName)
        if (value != null) {
            action(value)
        }
    }
}
