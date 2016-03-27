package com.github.mrduguo.gradle.samplelib

import spock.lang.Specification

class WorldSpec extends Specification {



    void "should say hello"() {
        def world=new World()

        expect:
        world.sayHello() == 'Hello My World'
    }


}