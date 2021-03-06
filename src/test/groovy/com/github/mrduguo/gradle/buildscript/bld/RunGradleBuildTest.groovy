package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest

class RunGradleBuildTest extends AbstractGradlewTest {

    def void testRun() {
        prepareGradleProject()
        if(Env.jobName()){
            runGradlewBuild('check')
        }else{
            runGradlewBuild(System.properties.testBuildParams ?: '')
        }
    }
}
