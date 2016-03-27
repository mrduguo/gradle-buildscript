## centralized gradle buildscript
A build system inspired by [spring boot auto configure|https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-auto-configuration.html]. Basically it adapt to your project source code structure and automatically apply the build logic which is offered by the build system. 


### the project build.gradle

```
buildscript {
    apply from: System.properties.buildscriptUrl ?: "${System.properties.mavenRepoUrl ?: 'https://dl.bintray.com/mrduguo/maven/'}com/github/mrduguo/gradle/buildscript/buildscript.gradle"
}
apply plugin: 'com.github.mrduguo.gradle.buildscript'
```