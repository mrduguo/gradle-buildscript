package com.github.mrduguo.gradle.buildscript.dist

import com.github.mrduguo.gradle.buildscript.jvm.JvmPlugin
import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProjectHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.credentials.AwsCredentials
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

class DistPlugin implements Plugin<Project> {

    public static boolean isBintrayRepoEnabled(){
        Env.config('distBintrayKey')
    }


    @Override
    void apply(Project project) {
        setupProjectRepository()
        project.ext.matchBuildscriptVersion=generateMatchVersion(project)
        if (project.file('src/main').exists()) {
            project.getPlugins().apply(MavenPublishPlugin)
            project.plugins.apply(com.jfrog.bintray.gradle.BintrayPlugin)
            project.afterEvaluate {
                if (ProjectHelper.isTaskExist('jar')) {
                    enableMavenPublish(project)
                }
            }
        }
    }

    def generateMatchVersion(Project project) {
        def baseVersion=project.ext.libBuildscriptVersion.split('-').first()
        def versionParts = baseVersion.split('\\.')
        def matchVersionParts=[]
        versionParts.eachWithIndex { String part, int i ->
            if((i+1)==versionParts.size()){
                matchVersionParts << part.toInteger()+1
            }else{
                matchVersionParts << part
            }
        }
        "(,${matchVersionParts.join('.')})"
    }

    def setupProjectRepository() {
        ProjectHelper.project.buildscript.repositories.each { repo ->
            ProjectHelper.project.repositories.add(repo)
            ProjectHelper.project.subprojects.each { subproject ->
                subproject.repositories.add(repo)
            }
        }
    }

    def enableMavenPublish(Project project) {
        if (!project.group) {
            project.group = Env.config('groupId')
            if (!project.group) {
                project.group = detectGroupIdBasedSource(project)
            }
        }
        def publishingExtension = project.extensions.findByName('publishing')
        setupPublishRepo(project, publishingExtension)


        if(!isBintrayRepoEnabled()){
            ProjectHelper.doIfTaskExist('build') { it.dependsOn('publish') }
        }
        doPublish(project, publishingExtension)

        ProjectHelper.doIfTaskExist('check') { def check ->
            ProjectHelper.getTask('publish').mustRunAfter check
        }
    }

    def doPublish(Project project, def publishingExtension) {
        publishingExtension.with {
            publications {
                maven(MavenPublication) {
                    from project.components.java
                    artifactId Env.artifactId()
                    ProjectHelper.doIfTaskExist('sourcesJar'){def sourcesJar->
                        artifact sourcesJar
                    }
                    if(project.file('src/main/resources/META-INF/gradle-plugins/com.github.mrduguo.gradle.buildscript.properties').exists()){
                        artifact('build/resources/main/com/github/mrduguo/gradle/buildscript/buildscript.gradle') {
                            classifier 'buildscript'
                        }
                    }
                }
            }
        }
    }

    def setupPublishRepo(Project project, def publishingExtension) {
        def mavenReleaseRepoUrl = Env.config('mavenReleaseRepoUrl')
        if (!mavenReleaseRepoUrl) {
            def localRepoUrl
            def s3RepoUrl
            project.buildscript.repositories.each { def repo ->
                def repoUrl = repo.url.toString()
                if (repoUrl.startsWith('https://s3')) {
                    s3RepoUrl = repoUrl
                } else if (repoUrl.startsWith('file')) {
                    localRepoUrl = repoUrl
                }
            }
            if (localRepoUrl) {
                mavenReleaseRepoUrl = localRepoUrl
            } else if (s3RepoUrl && Env.config('JOB_NAME')) {
                mavenReleaseRepoUrl = s3RepoUrl
            } else {
                mavenReleaseRepoUrl = "file://${project.file('build/repo')}"
            }
        }
        if (mavenReleaseRepoUrl.startsWith('https://s3')) {
            project.ext['awsS3RepoUrl'] = mavenReleaseRepoUrl
            project.ext['mavenReleaseRepoUrl'] = mavenReleaseRepoUrl
        }
        publishingExtension.with {
            repositories {
                maven {
                    if (mavenReleaseRepoUrl.startsWith('https://s3')) {
                        url mavenReleaseRepoUrl.replaceAll('https://s3-[^/]*/(.*)', 's3://$1')
                        if (System.getenv('AWS_CREDENTIAL_PROFILES_FILE')) {
                            credentials(AwsCredentials) {
                                new File(System.getenv('AWS_CREDENTIAL_PROFILES_FILE')).eachLine { String line ->
                                    def kv = line.split('=')
                                    if (kv.length == 2) {
                                        if (kv[0].trim() == 'aws_access_key_id') {
                                            accessKey kv[1].trim()
                                        } else if (kv[0].trim() == 'aws_secret_access_key') {
                                            secretKey kv[1].trim()
                                        }
                                    }
                                }
                            }
                        } else if (new File(System.getProperty('user.home'), '.aws/credentials').exists()) {
                            credentials(AwsCredentials) {
                                new File(System.getProperty('user.home'), '.aws/credentials').eachLine { String line ->
                                    def kv = line.split('=')
                                    if (kv.length == 2) {
                                        if (kv[0].trim() == 'aws_access_key_id') {
                                            accessKey kv[1].trim()
                                        } else if (kv[0].trim() == 'aws_secret_access_key') {
                                            secretKey kv[1].trim()
                                        }
                                    }
                                }
                            }
                        } else {
                            throw new RuntimeException('cannot find ~/.aws/credentials and gradle release does not support the AwsImAuthentication yet')
//                    authentication {
//                        awsIm(AwsImAuthentication)
//                    }
                        }
                    } else if(isBintrayRepoEnabled()){
                        enableBintrayDist(project)
                    } else {
                        url mavenReleaseRepoUrl
                    }
                }
            }
        }
    }

    def void enableBintrayDist(Project project) {
        def (defaultUser,defaultRepo)=parseBintrayUserAndRepo(project)
        def bintrayExtension = project.extensions.findByName('bintray')
        def distBintrayPackage = Env.config('distBintrayPackage', "${Env.artifactId()}")
        def userName = Env.config('distBintrayUser', defaultUser)
        def orgName = Env.config('distBintrayOrg', userName)
        bintrayExtension.with {
            user = userName
            key = Env.config('distBintrayKey')
            publications = ['maven']
            publish = true
            pkg {
                repo = Env.config('distBintrayRepo',defaultRepo)
                name = distBintrayPackage
                userOrg = orgName
                licenses = ['Apache-2.0']
                vcsUrl = Env.config('distBintrayVcsUrl',"https://github.com/${orgName}/${distBintrayPackage}.git")
            }
        }

        def bintrayUpload=ProjectHelper.getTask('bintrayUpload')
        ProjectHelper.getTask('build').dependsOn bintrayUpload
        bintrayUpload.dependsOn ProjectHelper.getTask('check')
    }

    def parseBintrayUserAndRepo(Project project) {
        def user,repoName
        project.buildscript.repositories.each { def repo ->
            def repoUrl = repo.url.toString()
            if (repoUrl.startsWith('https://dl.bintray.com/')) {
                def repoInfo=repoUrl.split('/')
                user=repoInfo[-2]
                repoName=repoInfo[-1]
            }
        }
        [user,repoName]
    }
    def detectGroupIdBasedSource(Project project) {
        def rootFolder = findProjectRootFolder(project.file('src/main/groovy'),0)
        if (rootFolder == null) {
            rootFolder = findProjectRootFolder(project.file('src/main/java'),0)
        }
        if (rootFolder == null) {
            return JvmPlugin.IS_SPRING_BOOT_PROJECT?'apps':'libs'
        } else {
            def (groupId,artifactId)=parseGroupAndArtifactId(rootFolder)
            return groupId
        }
    }

    def parseGroupAndArtifactId(File rootFolder) {
        def artifactId=rootFolder.name
        def groupParts=[]
        while(true){
            rootFolder=rootFolder.parentFile
            if(rootFolder.name=='groovy' || rootFolder.name=='java'){
                break
            }
            groupParts << rootFolder.name
        }
        return [groupParts.reverse().join('.'),artifactId]
    }

    def findProjectRootFolder(File parentFolder, int level) {
        int validFiles=0
        def subBolders=[]
        parentFolder.listFiles().each {File child->
            if(child.isDirectory()){
                if(child.name.indexOf('.')<0){
                    validFiles+=1
                    subBolders << child
                }
            }else if(child.name.endsWith('.groovy')||child.name.endsWith('.java')){
                validFiles+=2
            }
        }
        if(validFiles>1){
            if(level>1){
                return parentFolder
            }else{
                return null
            }
        }else{
            if(subBolders){
                return findProjectRootFolder(subBolders[0],level+1)
            }else{
                return null
            }
        }
    }

}