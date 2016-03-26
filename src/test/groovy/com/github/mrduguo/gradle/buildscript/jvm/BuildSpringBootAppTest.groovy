package com.github.mrduguo.gradle.buildscript.jvm

import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest
import org.apache.commons.io.FileUtils


/**

 ./gradlew -Dtest.single=BuildSpringBootAppTest

 (cd build/gradle-test-projects/BuildSpringBootAppTest_testRun && ./gradlew clean build aws_eb_deploy \
 -DJOB_NAME=lab-spring_boot_sample_project \
 -DmavenReleaseRepoUrl=https://s3-eu-west-1.amazonaws.com/elasticbeanstalk-eu-west-1-349318639323/maven-repo/ \
 -DawsEbSecurityGroups=external-facing-endpoint\
 -DawsEbKey=aws-public@statico.io\
 -DawsEbOverrideExistsEnv=true\
 -DawsEbHealthCheckEndpoint=/\
 -DawsEbDeleteVersionAfterTermination=true\
 -DawsEbTerminationDelayInMinutes=0\
 -DawsEbIamInstanceProfile=arn:aws:iam::349318639323:instance-profile/harvest-truested-server
 )


 ./gradlew -Dtest.single=BuildSpringBootAppTest

 (cd build/gradle-test-projects/BuildSpringBootAppTest_testRun && ./gradlew clean build aws_eb_deploy \
 -DJOB_NAME=lab-spring_boot_sample_project \
 -DmavenReleaseRepoUrl=https://s3-eu-west-1.amazonaws.com/elasticbeanstalk-eu-west-1-349318639323/maven-repo/ \
 -DawsEbSecurityGroups=external-facing-endpoint\
 -DawsEbKey=aws-public@statico.io\
 -DawsEbOverrideExistsEnv=true\
 -DawsEbHealthCheckEndpoint=/\
 -DawsEbDeleteVersionAfterTermination=true\
 -DawsEbTerminationDelayInMinutes=0\
 -DawsEbIamInstanceProfile=arn:aws:iam::349318639323:instance-profile/harvest-truested-server\
 -DawsEbEnvironmentType=LoadBalanced\
 -DawsEbHealthReportingSystemType=enhanced
 )

 ebssh 52.50.84.4


 */
class BuildSpringBootAppTest extends AbstractGradlewTest {

    def void testRun() {
        prepareGradleProject()
        def artifactId='spring_boot_sample_project'
        FileUtils.copyDirectory(new File("src/test/resources/$artifactId"),testProjectDir)
        runGradlewBuild("-DartifactId=$artifactId ${System.properties.testBuildParams ?: ''}")

        def releasedArtifactFolder=new File(testProjectDir,"build/repo/apps/$artifactId")
        assert releasedArtifactFolder.exists()

        File releasedVersionFolder
        releasedArtifactFolder.listFiles().each {File file->
            if(file.isDirectory()){
                releasedVersionFolder=file
            }
        }
        assert releasedVersionFolder.exists()


        assert new File(releasedVersionFolder,"$artifactId-${releasedVersionFolder.name}.jar").exists()
        assert !new File(releasedVersionFolder,"$artifactId-${releasedVersionFolder.name}-sources.jar").exists()
        assert new File(releasedVersionFolder,"$artifactId-${releasedVersionFolder.name}.pom").exists()
    }
}
