package com.github.mrduguo.gradle.buildscript.bld

import org.gradle.api.tasks.bundling.Zip

class BldPackageAsZipTask extends Zip {

    File zipFile

    BldPackageAsZipTask(){
        from(project.projectDir)
        include(
                'README**',
                'gradle/**',
                'gradle**',
                'build.gradle',
                'buildSrc/build.gradle',
                'buildSrc/src/**',
                'src/**',
        )
        zipFile=new File(project.buildDir,"distributions/${project.projectDir.name}-${new Date().format('yyyyMMdd')}.zip")
        setArchiveName(zipFile.name)
    }

    @Override
    protected void copy() {
        super.copy()
        println "assembled zip to: ${zipFile.absolutePath}"
    }

}
