package com.github.mrduguo.gradle.buildscript.aws

import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest
import com.amazonaws.services.s3.AmazonS3Client
import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.commons.lang.RandomStringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class AwsEbDeployTask extends DefaultTask {

    String appName
    String appEnvName
    String appEnvVersion
    String appS3ArtifactBaseUrl

    @TaskAction
    def deploy() {
        resolveAppS3ArtifactBaseUrl()
        resolveAppName()
        resolveAppEnvName()
        resolveAppEnvVersion()
        createEnv()
    }

    def resolveAppS3ArtifactBaseUrl() {
        appS3ArtifactBaseUrl = Env.config('appS3ArtifactBaseUrl')
        if (!appS3ArtifactBaseUrl) {
            appS3ArtifactBaseUrl = "${project.ext.awsS3RepoUrl}${Env.config('groupId', 'apps').split('\\\\.').join('/')}/${Env.artifactId()}"
        }
    }

    def resolveAppName() {
        appName = Env.config('awsEbAppName')
        if (!appName) {
            appName = Env.jobName('lab').split('-')[0]
        }
    }

    def resolveAppEnvName() {
        appEnvName = Env.config('awsEbAppEnvName')
        if (!appEnvName) {
            appEnvName = "$appName-${Env.artifactId().split('_').collect { it.substring(0, 1) }.join('')}"
        }
    }

    def resolveAppEnvVersion() {
        appEnvVersion = Env.config('awsEbAppEnvVersion')
        if (!appEnvVersion) {
            def awsEbArtifactVersion = Env.config('awsEbArtifactVersion', 'LATEST')
            if (awsEbArtifactVersion == 'LATEST') {
                if (ProjectHelper.isTaskExist('publish')) {
                    awsEbArtifactVersion = project.version
                } else {
                    awsEbArtifactVersion = resolveLatestVersion()
                }
            }
            def awsEbS3ArchivePath = "${appS3ArtifactBaseUrl}/${awsEbArtifactVersion}/${Env.artifactId()}-${awsEbArtifactVersion}.jar"
            println "awsEbS3ArchivePath: $awsEbS3ArchivePath"

            def (s3Bucket, s3Key) = resolveS3BucketAndKey(awsEbS3ArchivePath)
            appEnvVersion = "${appEnvName}-${awsEbArtifactVersion}"
            AwsEbHelper.awsEbCreateVersion(appName, appEnvVersion, s3Bucket, s3Key)
        }
    }

    def resolveLatestVersion() {
        def awsEbS3FileVersion = null
        def s3client = new AmazonS3Client()
        def (String s3Bucket, String s3Key) = resolveS3BucketAndKey("${appS3ArtifactBaseUrl}/maven-metadata.xml")
        s3client.getObject(s3Bucket, s3Key).objectContent.text.eachLine { String versionLine ->
            if (versionLine.endsWith('</version>')) {
                String simpleVersion = versionLine.substring(versionLine.indexOf('>') + 1, versionLine.indexOf('<', 15))
                if (awsEbS3FileVersion == null || awsEbS3FileVersion < simpleVersion) {
                    awsEbS3FileVersion = simpleVersion
                }
            }
        }
        println "latest version: $awsEbS3FileVersion"
        awsEbS3FileVersion
    }

    def createEnv(n) {
        def existEnv = AwsEbHelper.awsEbDescribeEnv(appName, null, appEnvName)
        if (existEnv) {
            if (Env.config('awsEbOverrideExistsEnv') == 'true') {
                def tmpEnvName = "${appEnvName}-${Env.config('BUILD_NUMBER') ?: Env.config('TRAVIS_BUILD_NUMBER') ?: RandomStringUtils.randomAlphanumeric(4).toLowerCase()}"
                def newEnv = performCreateEnv(tmpEnvName)
                AwsEbHelper.awsEbSwapEnvs(newEnv, existEnv)
                healthCheck(appName, appEnvName)
                AwsEbHelper.awsEbTerminateEnv(existEnv)
            } else {
                throw new RuntimeException("env $appEnvName already exist")
            }
        } else {
            performCreateEnv(appEnvName)
        }
    }

    def performCreateEnv(String appEnvName) {
        def ebEnv = AwsEbHelper.awsEbCreateEnv(appName, appEnvVersion, appEnvName, { CreateEnvironmentRequest request ->
            def optionsSettings = []

            optionsSettings << AwsEbHelper.awsEbConfigOption('aws:autoscaling:launchconfiguration', 'InstanceType', Env.config('awsEbInstanceType', 't2.small'))

            Env.doIfHasConfig('awsEbHealthReportingSystemType') {
                optionsSettings << AwsEbHelper.awsEbConfigOption('aws:elasticbeanstalk:healthreporting:system', 'SystemType', it)
                optionsSettings << AwsEbHelper.awsEbConfigOption('aws:elasticbeanstalk:environment', 'ServiceRole', Env.config('awsEbServiceRole', 'aws-elasticbeanstalk-service-role'))
            }
            def appNameInfo = appName.split('_')
            optionsSettings << AwsEbHelper.awsEbConfigOption('aws:elasticbeanstalk:application:environment', 'SERVER_PORT', Env.config('SERVER_PORT', '5000'))
            optionsSettings << AwsEbHelper.awsEbConfigOption('aws:elasticbeanstalk:application:environment', 'ENV_NAME', appNameInfo[0])
            if (appNameInfo.length > 1) {
                optionsSettings << AwsEbHelper.awsEbConfigOption('aws:elasticbeanstalk:application:environment', 'ENV_VERSION', appNameInfo[1])
            }

            Env.doIfHasConfig('awsEbKey') {
                optionsSettings << AwsEbHelper.awsEbConfigOption('aws:autoscaling:launchconfiguration', 'EC2KeyName', it)
            }

            optionsSettings << AwsEbHelper.awsEbConfigOption('aws:elasticbeanstalk:environment', 'EnvironmentType', Env.config('awsEbEnvironmentType', 'SingleInstance'))
            // awsEbEnvironmentType=SingleInstance
            // awsEbEnvironmentType=LoadBalanced - default

            Env.doIfHasConfig('awsEbIamInstanceProfile') {
                optionsSettings << AwsEbHelper.awsEbConfigOption('aws:autoscaling:launchconfiguration', 'IamInstanceProfile', it)
            }
            Env.doIfHasConfig('awsEbSecurityGroups') {
                optionsSettings << AwsEbHelper.awsEbConfigOption('aws:autoscaling:launchconfiguration', 'SecurityGroups', it)
            }
            request.withOptionSettings(optionsSettings)
        })
        healthCheck(appName, appEnvName)
        ebEnv
    }

    def healthCheck(def appName, def appEnvName) {
        def ebEnv = AwsEbHelper.awsEbDescribeEnv(appName, null, appEnvName)
        def awsEbHealthCheckEndpoint = Env.config('awsEbHealthCheckEndpoint')
        if (awsEbHealthCheckEndpoint) {
            String healthCheckUrl = "http://${ebEnv.getCNAME()}$awsEbHealthCheckEndpoint"
            println "health checking url: $healthCheckUrl ..."

            def restClient = new RESTClient(healthCheckUrl)
            restClient.client.params.setParameter("http.socket.timeout", 60000)
            restClient.client.params.setParameter("http.connection.timeout", 60000)
            restClient.client.params.setParameter("http.protocol.handle-redirects", false)
            restClient.get(contentType: ContentType.TEXT) { resp, reader ->
                try {
                    assert resp.status == 200
                } catch (Throwable ex) {
                    try {
                        println "STATUS CODE: $resp.status"
                        resp.responseBase.headergroup.headers.each {
                            println "< $it"
                        }
                        println "response text: \n$reader.text"
                    } catch (Throwable ignore) {
                    }
                    throw ex
                }

                def awsEbHealthCheckExpect = Env.config('awsEbHealthCheckExpect')
                if (awsEbHealthCheckExpect) {
                    assert reader.text.contains(awsEbHealthCheckExpect)
                }
                println "health check $healthCheckUrl PASSED"
            }
        }
        //TODO: system verification test start here?
    }

    def resolveS3BucketAndKey(String s3Url) {
        def (matchedString, s3Bucket, s3Key) = (s3Url =~ /https:\/\/[^\/]*\/([^\/]*)\/(.*)/)[0]
        return [s3Bucket, s3Key]
    }

}