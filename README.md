## centralized gradle buildscript [![Build Status](https://travis-ci.org/mrduguo/gradle-buildscript.svg?branch=master)](https://travis-ci.org/mrduguo/gradle-buildscript)
A build system inspired by [spring boot auto configure](https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-auto-configuration.html). Basically it adapt to your project source code structure and automatically apply the build logic which is offered by the build system. By this way, your project will be clean and you can focus more on feature development. On other side, build engineer have full responsibility and control on the build system.


### the project build.gradle

```
buildscript {
    apply from: System.properties.buildscriptUrl ?: "${System.properties.mavenRepoUrl ?: 'https://dl.bintray.com/mrduguo/maven/'}com/github/mrduguo/gradle/buildscript/buildscript.gradle"
}
apply plugin: 'com.github.mrduguo.gradle.buildscript'
```



### sample projects

* [gradle-simplest-project](https://github.com/mrduguo/gradle-simplest-project) - a simplest gradle build project
* [gradle-sample-lib](https://github.com/mrduguo/gradle-sample-lib) - a demo for lib project
* [gradle-sample-app](https://github.com/mrduguo/gradle-sample-app) - a demo for spring boot based app project
* [gradle-sample-cucumber](https://github.com/mrduguo/gradle-sample-cucumber) - a demo for cucumber groovy based integration test
