package com.github.mrduguo.gradle.buildscript

import com.github.mrduguo.gradle.buildscript.utils.ProcessRunner
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

abstract class AbstractGradlewTest extends GroovyTestCase {

    def File testProjectDir

    @Override
    void setUp() {
        super.setUp()
        testProjectDir=new File("build/gradle-test-projects/${getClass().simpleName}_$name")
        testProjectDir.mkdirs()
    }

    def void prepareGradleProject() {

        def wrapperDir=new File(testProjectDir,'gradle/wrapper')
        wrapperDir.mkdirs()
        FileUtils.copyDirectory(new File(testProjectDir,'../../../gradle/wrapper'),wrapperDir)

        def dependenciesText=[]
        new File('gradle/dependencies.gradle').eachLine {String line->
            if(line.contains('gradleApi')){
                dependenciesText << "    classpath files('../../../build/classes/main')"
                dependenciesText << "    classpath files('../../../build/resources/main')"
                return
            }else if(line.contains('testCompile')){
                return
            }else if(line.contains('compile \'')){
                line = line.replace('compile','classpath')
            }
            dependenciesText << line
        }

        def templateBuildGradle=[]
        getClass().getResourceAsStream('/com/github/mrduguo/gradle/buildscript/simplest-project-build.gradle').eachLine {def line->
            if(line.startsWith('buildscript') || line.startsWith('    apply') || line.startsWith('}')){
                line="//$line"
            }
            templateBuildGradle << line
        }
        def testBuildGradle="""\
buildscript{
${dependenciesText.collect {"    $it"}.join('\n')}
}
${templateBuildGradle.join('\n')}\
"""
        def buildFile=new File(testProjectDir,'build.gradle')
        buildFile.write testBuildGradle

        Files.copy(Paths.get(new File(testProjectDir,'../../../gradlew').toURI()), Paths.get(new File(testProjectDir,'gradlew').toURI()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)


        println "prepared gradle project to: $testProjectDir.absolutePath"
    }

    def runGradlewBuild(def args=''){
        def processRunner=new ProcessRunner(dir: testProjectDir,cmd:"./gradlew $args")
        processRunner.execute()
    }

    def assertFileEquals(File actual,File expected){
        try{
            assertEquals(actual.text,expected.text)
        }catch (Throwable ex){
            println """files not the same:
acutal $actual.absolutePath
expected $expected.absolutePath
"""
            throw ex
        }
    }
}
