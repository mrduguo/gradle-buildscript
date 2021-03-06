package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest

class BldUpgradeTaskTest extends AbstractGradlewTest {

    def void testRunTask() {
        prepareGradleProject()
        runGradlewBuild("bld_upgrade -DtargetProjectRoot=${Env.config('targetProjectRoot','build/test-root')}")
    }

}
