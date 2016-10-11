## Centralized Adaptive Build System [![Build Status](https://travis-ci.org/mrduguo/gradle-buildscript.svg?branch=master)](https://travis-ci.org/mrduguo/gradle-buildscript)
A build system inspired by [spring boot auto configure](https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-auto-configuration.html). Basically it adapt to your project source code structure and automatically apply the build logic which is offered by the build system. By this way, your project will be clean and you can focus more on feature development. On other side, build engineer have full responsibility and control on the build system.


### Your Project build.gradle To Use The Build System

```
buildscript {
    apply from: System.properties.buildscriptUrl ?: System.getenv().buildscriptUrl ?: project.hasProperty('buildscriptUrl') ? project.ext.buildscriptUrl : 'https://dl.bintray.com/mrduguo/maven/com/github/mrduguo/gradle/gradle-buildscript/buildscript.gradle'
}
apply plugin: 'com.github.mrduguo.gradle.buildscript'
```



### Usage

#### npm project

A `package.json` or `src/main/webapp/package.json` file will active the npm project support:

1. automatically install nodejs version based on `package.json@engines.node` with [nvm](https://github.com/creationix/nvm) (except Windows)
2. manage `node_modules` life cycle
  1. delete the folder and run `npm install` if anything change from `package.json`
  2. apply override from `config/override/node_modules` if exist
3. `npm` command bind
  1. execute `npm run build` with default build command `./gradlew`
  2. execute `npm start` with when execute `./gradlew run`

### Sample Projects

* [gradle-simplest-project](https://github.com/mrduguo/gradle-simplest-project) - simplest project only contain build system
* [gradle-sample-lib](https://github.com/mrduguo/gradle-sample-lib) - library project which publish jar file
* [gradle-sample-app](https://github.com/mrduguo/gradle-sample-app) - spring boot based app come with zone down time AWS deployment work flow
* [gradle-sample-app](https://github.com/mrduguo/gradle-sample-cmd) - spring boot based command line app with docker support
* [gradle-sample-react](https://github.com/mrduguo/gradle-sample-react) - react (UI) + spring boot (REST API)
* [gradle-sample-cucumber](https://github.com/mrduguo/gradle-sample-cucumber) - cucumber groovy based integration test


### The Only Requirement

* JAVA 7 or newer


### Standard Commands

#### Build Locally

```
./gradlew
```

All projects, include this project itself, will do the full build by default. 
It will do as much as close to jenkins integration build.

#### Run Locally

```
./gradlew run
```

You may use the `run` task on runnable project for local development. Such as:

* spring boot application - will run the application
* npm js application - will run the npm in watch mode for local development
