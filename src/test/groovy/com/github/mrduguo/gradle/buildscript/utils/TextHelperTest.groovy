package com.github.mrduguo.gradle.buildscript.utils



class TextHelperTest extends spock.lang.Specification {
    def "file inplace replace text"() {
        given:
        def testFile=File.createTempFile('test-','.txt')
        testFile.write('foo=bar')

        expect:
        TextHelper.replaceText(testFile,'foo','FOO').text=='FOO=bar'
    }
}
