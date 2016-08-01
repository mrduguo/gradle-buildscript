package com.github.mrduguo.gradle.buildscript.utils

import java.nio.file.Paths

class Env {

    public static String config(String key, String defaultValue = null) {
        def result = System.properties[key] ?: System.getenv(key)
        if (result) {
            return result
        }
        if (ProjectHelper.project && ProjectHelper.project.hasProperty(key)) {
            result = ProjectHelper.project[key]
        }
        result ?: defaultValue
    }

    public static String jobName(String defaultName = null) {
        config('JOB_NAME',config('TRAVIS_JOB_ID', defaultName))
    }

    public static Map gitInfo() {
        if (config('GIT_BRANCH') != null) {
            [
                    branch: config('GIT_BRANCH'),
                    commit: config('GIT_COMMIT')?.substring(0, 7),
                    buildNumber: config('BUILD_NUMBER'),
            ]
        } else if (config('TRAVIS_BRANCH') != null) {
            [
                    branch: config('TRAVIS_BRANCH'),
                    commit: config('TRAVIS_COMMIT')?.substring(0, 7),
                    buildNumber: config('TRAVIS_BUILD_NUMBER'),
            ]
        }else if (ProjectHelper.project.file('.git').exists()) {
            try{
                def dirtyFlag='git diff-files'.execute().text.trim().length()>0?'_dirty':''
                [
                        branch: 'git rev-parse --abbrev-ref HEAD'.execute().text.trim(),
                        commit: 'git rev-parse --short HEAD'.execute().text.trim()+dirtyFlag,
                        buildNumber: config('BUILD_NUMBER','0'),
                ]
            }catch (Exception ex){
                println "failed to execute git command: $ex.message"
                ex.printStackTrace()
            }
        }
    }

    public static String artifactId() {
        def name = config('artifactId')
        if (!name) {
            name = new File(Paths.get('').toFile().absolutePath).name
        }
        name
    }


    public static void doIfHasConfig(def configName, def action) {
        def value = config(configName)
        if (value != null) {
            action(value)
        }
    }



    public static List<String> sortVersions(List<String> versions) {
        versions.sort(false) { a, b ->
            compareVersion(a, b)
        }
    }

    public static int compareVersion(String a, String b) {
        List verA = a.replaceAll('-', '.').replaceAll('[^\\d\\.]', '.').tokenize('.')
        List verB = b.replaceAll('-', '.').replaceAll('[^\\d\\.]', '').tokenize('.')
        def commonIndices = Math.min(verA.size(), verB.size())
        for (int i = 0; i < commonIndices; ++i) {
            def numA = verA[i].toLong()
            def numB = verB[i].toLong()
            if (numA != numB) {
                return numA <=> numB
            }
        }
        if (verA.size() != verB.size()) {
            verA.size() <=> verB.size()
        } else {
            a <=> b
        }
    }
}
