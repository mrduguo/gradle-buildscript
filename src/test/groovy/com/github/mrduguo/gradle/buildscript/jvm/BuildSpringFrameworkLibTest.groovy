package com.github.mrduguo.gradle.buildscript.jvm

import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest
import org.apache.commons.io.FileUtils

class BuildSpringFrameworkLibTest extends AbstractGradlewTest {

    def void testRun() {
        prepareGradleProject()
        def artifactId='spring_framework_sample_project'
        FileUtils.copyDirectory(new File("src/test/resources/$artifactId"),testProjectDir)
        runGradlewBuild("-DartifactId=$artifactId ${System.properties.testBuildParams ?: ''}")

        def releasedArtifactFolder=new File(testProjectDir,"build/repo/libs/$artifactId")
        assert releasedArtifactFolder.exists()

        File releasedVersionFolder
        releasedArtifactFolder.listFiles().each {File file->
            if(file.isDirectory()){
                releasedVersionFolder=file
            }
        }
        assert releasedVersionFolder.exists()


        assert new File(releasedVersionFolder,"$artifactId-${releasedVersionFolder.name}.jar").exists()
        assert new File(releasedVersionFolder,"$artifactId-${releasedVersionFolder.name}-sources.jar").exists()

        def pomFile = new File(releasedVersionFolder, "$artifactId-${releasedVersionFolder.name}.pom")
        assert pomFile.exists()
        assert pomFile.text.contains('groovy-all')
    }
}
