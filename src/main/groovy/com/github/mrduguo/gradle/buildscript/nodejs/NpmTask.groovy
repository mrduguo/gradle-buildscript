package com.github.mrduguo.gradle.buildscript.nodejs

import com.github.mrduguo.gradle.buildscript.utils.Env
import com.github.mrduguo.gradle.buildscript.utils.ProcessRunner
import com.google.gson.Gson
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class NpmTask extends DefaultTask {

    File workingDir
    def npmCmds
    def npmRunCmd
    def runInBackground = false

    File packageJsonFile
    def cmds

    @TaskAction
    def run() {
        prepareBuildFolder()
        generateCmds()
        if (runInBackground) {
            Thread.start {
                runNpm()
            }
        } else {
            runNpm()
        }
    }

    def runNpm() {
        new ProcessRunner(
                timeoutInMilliSeconds: Long.MAX_VALUE,
                dir: workingDir,
                cmds: cmds,
        ).execute()
    }

    def generateCmds() {
        cmds = []
        def packageJson = new Gson().fromJson(packageJsonFile.text, Map)
        if(Os.isFamily(Os.FAMILY_WINDOWS)){
            // have to run npm command once a time as it exit the process after each command
            cmds << "cd $workingDir.absolutePath"
            cmds << "npm install"
            runNpm()

            cmds = []
            cmds << "cd $workingDir.absolutePath"
        }else{
            def nodeVersion = detectNodeVersion(packageJson)
            def nvmFile = new File(System.getProperty('user.home'), '.nvm/nvm.sh')
            if (!nvmFile.exists()) {
                cmds << "curl -o- https://raw.githubusercontent.com/creationix/nvm/${Env.config('nvmVersion', 'v0.30.2')}/install.sh | bash"
            }
            cmds << '. ~/.nvm/nvm.sh'
            cmds << "nvm install $nodeVersion\n"
            cmds << "cd $workingDir.absolutePath"
            cmds << "npm install"
        }

        if (npmCmds) {
            npmCmds.each {
                cmds << it
            }
        }

        if (!npmRunCmd) {
            npmRunCmd = Env.config('npmRunCmd')
            if (!npmRunCmd) {
                if(packageJson.scripts?.containsKey('build')){
                    npmRunCmd='build'
                }else{
                    npmRunCmd = packageJson.scripts?.keySet().first()
                }
            }
        }
        if (npmRunCmd) {
            cmds << "npm run $npmRunCmd"
        }
    }

    String detectNodeVersion(def packageJson) {
        packageJson.engines?.node ?: 'node'
    }

    def outputFolders() {
        [new File(workingDir, 'node_modules')]
    }

    def prepareBuildFolder() {
        packageJsonFile = new File(workingDir, 'package.json')
        assert packageJsonFile.exists()
    }
}
