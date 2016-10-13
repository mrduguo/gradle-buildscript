package com.github.mrduguo.gradle.buildscript.jvm

import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest

class BuildScalaAppTest extends AbstractGradlewTest {

    def void testRun() {
        prepareGradleProject()
        runGradlewBuild()

        def releasedArtifactFolder=new File(testProjectDir,"build/repo/libs/BuildScalaAppTest")
        assert releasedArtifactFolder.exists()
    }
}
