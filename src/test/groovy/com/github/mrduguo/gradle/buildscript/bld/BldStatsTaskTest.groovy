package com.github.mrduguo.gradle.buildscript.bld

import com.github.mrduguo.gradle.buildscript.AbstractGradlewTest

class BldStatsTaskTest extends AbstractGradlewTest {

    def void testCreateTask() {
        prepareGradleProject()
        runGradlewBuild('bld_stats')
    }
}
