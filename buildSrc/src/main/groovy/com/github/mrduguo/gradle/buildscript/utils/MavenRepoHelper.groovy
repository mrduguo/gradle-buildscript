package com.github.mrduguo.gradle.buildscript.utils

import com.github.mrduguo.gradle.buildscript.jvm.JvmPlugin
import org.gradle.api.credentials.AwsCredentials
import org.gradle.api.publish.maven.MavenPublication

class MavenRepoHelper {

    static def setupProjectRepository() {
        ProjectHelper.project.buildscript.repositories.each { repo ->
            ProjectHelper.project.repositories.add(repo)
            ProjectHelper.project.subprojects.each { subproject ->
                subproject.repositories.add(repo)
            }
        }
    }


    static def enableMavenPublish() {
        ProjectHelper.project.group = Env.config('groupId', JvmPlugin.IS_SPRING_BOOT_PROJECT ? 'apps' : 'libs')
        def publishingExtension = ProjectHelper.project.extensions.findByName('publishing')
        setupPublishRepo(publishingExtension)


        ProjectHelper.doIfTaskExist('jar') {
            ProjectHelper.doIfTaskExist('build') { it.dependsOn('publish') }
            doPublishMain(publishingExtension)
            if (!JvmPlugin.IS_SPRING_BOOT_PROJECT) {
                File jarFileSources = new File(ProjectHelper.project.buildDir, "libs/${Env.artifactId()}-${ProjectHelper.project.version}-sources.jar")
                doPublish(publishingExtension, jarFileSources, 'jar', 'sources')
            }
        }

        //TODO move to plugin
        ProjectHelper.doIfTaskExist('check') { def check ->
            ProjectHelper.getTask('publish').mustRunAfter check
        }
        ProjectHelper.doIfTaskExist('publishInitScript') { def publishInitScript ->
            publishInitScript.dependsOn ProjectHelper.getTask('publish')
            ProjectHelper.getTask('build').dependsOn publishInitScript
        }
    }

    static def doPublishMain(def publishingExtension) {
        publishingExtension.with {
            publications {
                mavenJava(MavenPublication) {
                    from ProjectHelper.project.components.java
                    artifactId Env.artifactId()
                }
            }
        }
    }

    static def doPublish(def publishingExtension, def file, def extensionType, def classifierName = null) {
        publishingExtension.with {
            publications {
                mavenJava(MavenPublication) {
                    artifact(file) {
                        classifier classifierName
                        artifactId Env.artifactId()
                        extension extensionType
                    }
                }
            }
        }
    }

    static def setupPublishRepo(def publishingExtension) {
        def mavenReleaseRepoUrl = Env.config('mavenReleaseRepoUrl')
        if (!mavenReleaseRepoUrl) {
            def localRepoUrl
            def s3RepoUrl
            ProjectHelper.project.buildscript.repositories.each { def repo ->
                def repoUrl = repo.url.toString()
                if (repoUrl.startsWith('https://s3')) {
                    s3RepoUrl = repoUrl
                } else if (repoUrl.startsWith('file')) {
                    localRepoUrl = repoUrl
                }
            }
            if (localRepoUrl) {
                mavenReleaseRepoUrl = localRepoUrl
            } else if (s3RepoUrl && Env.config('JOB_NAME')) {
                mavenReleaseRepoUrl = s3RepoUrl
            } else {
                mavenReleaseRepoUrl = "file://${ProjectHelper.project.file('build/repo')}"
            }
        }
        if (mavenReleaseRepoUrl.startsWith('https://s3')) {
            ProjectHelper.project.ext['awsS3RepoUrl'] = mavenReleaseRepoUrl
        }
        publishingExtension.with {
            repositories {
                maven {
                    if (mavenReleaseRepoUrl.startsWith('https://s3')) {
                        url mavenReleaseRepoUrl.replaceAll('https://s3-[^/]*/(.*)', 's3://$1')
                        if (System.getenv('AWS_CREDENTIAL_PROFILES_FILE')) {
                            credentials(AwsCredentials) {
                                new File(System.getenv('AWS_CREDENTIAL_PROFILES_FILE')).eachLine { String line ->
                                    def kv = line.split('=')
                                    if (kv.length == 2) {
                                        if (kv[0].trim() == 'aws_access_key_id') {
                                            accessKey kv[1].trim()
                                        } else if (kv[0].trim() == 'aws_secret_access_key') {
                                            secretKey kv[1].trim()
                                        }
                                    }
                                }
                            }
                        } else if (new File(System.getProperty('user.home'), '.aws/credentials').exists()) {
                            credentials(AwsCredentials) {
                                new File(System.getProperty('user.home'), '.aws/credentials').eachLine { String line ->
                                    def kv = line.split('=')
                                    if (kv.length == 2) {
                                        if (kv[0].trim() == 'aws_access_key_id') {
                                            accessKey kv[1].trim()
                                        } else if (kv[0].trim() == 'aws_secret_access_key') {
                                            secretKey kv[1].trim()
                                        }
                                    }
                                }
                            }
                        } else {
                            throw new RuntimeException('gradle 2.9 release does not support the AwsImAuthentication yet')
//                    authentication {
//                        awsIm(AwsImAuthentication)
//                    }
                        }
                    } else {
                        url mavenReleaseRepoUrl
                    }
                }
            }
        }
    }

    static def collectBestMatchVersions(def bestMatchVersions, def allAvailableVersions, String baseVersion) {
        allAvailableVersions.each { String version ->
            if (version.startsWith(baseVersion)) {
                bestMatchVersions.add(version)
            }
        }
        if (bestMatchVersions.size() == 0 && baseVersion.lastIndexOf('.') > 0) {
            collectBestMatchVersions(bestMatchVersions, allAvailableVersions, baseVersion.substring(0, baseVersion.lastIndexOf('.')))
        }
        bestMatchVersions
    }
}
