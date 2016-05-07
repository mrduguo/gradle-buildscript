package com.github.mrduguo.gradle.buildscript.aws

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.*
import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.TimeHelper

class AwsEbHelper {

    public static AWSElasticBeanstalkClient awsEbClient() {
        def awsElasticBeanstalkClient = new AWSElasticBeanstalkClient()
        awsElasticBeanstalkClient.setRegion(RegionUtils.getRegion(Env.config('awsRegion', 'eu-west-1')))
        awsElasticBeanstalkClient
    }

    public static void awsEbCreateVersion(def appName, def versionLabel, def s3Bucket, def s3Key) {
        println("creating elastic beanstalk app ${appName} versionLabel ${versionLabel}")
        awsEbClient().createApplicationVersion(
                new CreateApplicationVersionRequest(
                        applicationName: appName,
                        versionLabel: versionLabel,
                        sourceBundle: new S3Location(
                                s3Bucket: s3Bucket,
                                s3Key: s3Key,
                        ),
                )
        )
        println("created application version ${versionLabel}")
    }

    public static EnvironmentDescription awsEbCreateEnv(def appName, def versionLabel, def envName, def customRequest) {
        println("creating elastic beanstalk env ${envName} ...")

        def createEnvironmentRequest = new CreateEnvironmentRequest(
                applicationName: appName,
                versionLabel: versionLabel,
                environmentName: envName,
                cNAMEPrefix: envName,
                solutionStackName: Env.config('solutionStackName', '64bit Amazon Linux 2016.03 v2.1.1 running Java 8'),
        )
        if (customRequest != null) {
            customRequest(createEnvironmentRequest)
        }
        awsEbClient().createEnvironment(createEnvironmentRequest)
        waitForEbEnvStatus(appName, null, envName, ['Ready'], { foundResult ->
            "created eb environment http://${foundResult.getCNAME()}/ ${foundResult.endpointURL}"
        })
    }

    public static void awsEbSwapEnvs(EnvironmentDescription newEnv, EnvironmentDescription existEnv) {
        println "swaping envs ${newEnv.environmentName} to ${existEnv.environmentName} ..."
        awsEbClient().swapEnvironmentCNAMEs(new SwapEnvironmentCNAMEsRequest().withSourceEnvironmentId(newEnv.environmentId).withDestinationEnvironmentId(existEnv.environmentId))
        newEnv = waitForEbEnvStatus(newEnv.applicationName, newEnv.environmentId, null, [newEnv.status])
        existEnv = waitForEbEnvStatus(existEnv.applicationName, existEnv.environmentId, null, [existEnv.status])

        println "swaped old env ${existEnv.environmentId} to ${newEnv.environmentName} at http://${existEnv.CNAME}/"
        println "  with new env ${newEnv.environmentId} as ${existEnv.environmentName} at http://${newEnv.CNAME}/"
    }

    public static void awsEbTerminateEnv(EnvironmentDescription existEnv) {
        def terminationDelay = Env.config('awsEbTerminationDelayInMinutes', '5')
        println "waiting for ${terminationDelay} minutes before terminate old environment ..."
        Thread.sleep(terminationDelay.toLong() * 60 * 1000)
        println("terminating elastic beanstalk env ${existEnv.environmentName} ...")
        awsEbClient().terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentId(existEnv.environmentId))
        waitForEbEnvStatus(existEnv.applicationName, existEnv.environmentId, null, ['NOT_FOUND'], { foundResult ->
            "terminated eb environment ${existEnv.environmentName}"
        })

        if (Env.config('awsEbDeleteVersionAfterTermination', 'false') == 'true') {
            awsEbDeleteVersion(existEnv.applicationName, existEnv.getVersionLabel())
        }
    }

    public static void awsEbDeleteVersion(String appName, String versionLabel) {
        awsEbClient().deleteApplicationVersion(new DeleteApplicationVersionRequest().withApplicationName(appName).withVersionLabel(versionLabel).withDeleteSourceBundle(false))
        println "deleted app ${appName} version ${versionLabel}"
    }


    public static EnvironmentDescription awsEbDescribeEnv(def appName, def environmentId = null, def envCname = null) {
        EnvironmentDescription ebEnv = null
        def describeEnvironmentsRequest = new DescribeEnvironmentsRequest().withApplicationName(appName).withIncludeDeleted(false)
        if (environmentId) {
            describeEnvironmentsRequest.withEnvironmentIds(environmentId)
        }
        awsEbClient().describeEnvironments(describeEnvironmentsRequest).environments.each { EnvironmentDescription foundEnvironment ->
            if (!envCname || foundEnvironment.getCNAME().startsWith(envCname + '.')) {
                ebEnv = foundEnvironment
            }
        }
        ebEnv
    }


    public static def waitForEbEnvStatus(
            def appName, def envId, def envCname, List expectedStatus, def successNotice = null) {
        TimeHelper.waitForActionInMinutes({
            EnvironmentDescription ebEnv = awsEbDescribeEnv(appName, envId, envCname)
            if (ebEnv == null) {
                if (expectedStatus.contains('NOT_FOUND')) {
                    return 'deleted'
                }
            } else if (expectedStatus.contains(ebEnv.status)) {
                return ebEnv
            }
            print "eb environment ${envId ? (envCname ? envId + ':' + envCname : envId) : envCname} status ${ebEnv?.status}"
        }, successNotice)
    }


    public static def awsEbConfigOption(String namespace, String optionName, String value) {
        new ConfigurationOptionSetting(
                namespace: namespace,
                optionName: optionName,
                value: value,
        )
    }


}