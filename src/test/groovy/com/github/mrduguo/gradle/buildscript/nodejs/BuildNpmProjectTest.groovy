package com.github.mrduguo.gradle.buildscript.nodejs

import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest
import com.github.mrduguo.gradle.buildscript.utils.ProcessRunner

/**

 ./gradlew -Dtest.single=BuildNpmProjectTest

 */
//@Ignore
class BuildNpmProjectTest extends AbstractGradlewTest {

    def void testRun() {
        prepareGradleProject()
        ProcessRunner processRunner=runGradlewBuild()
        assert processRunner.output.contains('npm install')
        assert processRunner.output.contains('Hello World: Who is this?')
        processRunner=runGradlewBuild()
        assert !processRunner.output.contains('npm install')
    }
}
