package com.github.mrduguo.gradle.buildscript.utils

import org.apache.commons.lang.SystemUtils
import org.codehaus.plexus.util.Os

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ProcessRunner {
    String cmd
    String[] cmds
    File dir
    long timeoutInMilliSeconds = 10 * 60 * 1000 // 10 minutes
    def output = new StringBuilder()
    def logOutput = true
    Process process

    static void run(String... commands) {
        if (commands.length == 1) {
            new ProcessRunner(cmd: commands[0]).execute()
        } else {
            new ProcessRunner(cmds: commands).execute()
        }
    }

    ProcessRunner execute() {
        File cmdFile = initCmd()


        runProcessAndConsumeOutput()

        if (!logOutput && !success) {
            println(output.toString())
        }
        if (!success) {
            throw new RuntimeException(isTimeout() ? "execute $cmd timeout" : "failed to execute: $cmd")
        }
        if (cmdFile) {
            cmdFile.delete()
        }
        this
    }

    private void runProcessAndConsumeOutput() {
        createProcess()
        def streamClosedCounter = new CountDownLatch(2)
        Thread.start {
            process.in.eachLine { line ->
                logLine('STD', line)
                output.append(line)
                output.append("\n")
            }
            streamClosedCounter.countDown()
        }
        Thread.start {
            process.err.eachLine { line ->
                logLine('ERR', line)
                output.append(line)
                output.append("\n")
            }
            streamClosedCounter.countDown()
        }
        process.waitForOrKill(timeoutInMilliSeconds)
        if (!streamClosedCounter.await(5, TimeUnit.SECONDS)) {
            if (!logOutput) {
                println(output.toString())
            }
            println("wait process output timeout after process finished 5 seconds with status ${process.exitValue()}")
        }
    }

    def createProcess() {
        process = cmd.execute((String[]) null, dir)
    }

    def initCmd() {
        def cmdFile
        if (cmds) {
            cmdFile = File.createTempFile('cmd-', SystemUtils.IS_OS_WINDOWS?'.bat':'.sh')
            cmdFile.write(cmds.join('\n'))
            cmd = "${SystemUtils.IS_OS_WINDOWS?'cmd /c':'bash'} $cmdFile.absolutePath"
            cmdFile
            if(logOutput){
                println("executing commands: $cmd\n${cmdFile.text}\n\n")
            }
        } else {
            logCommand()
        }
        cmdFile
    }

    def logCommand() {
        if(logOutput){
            println("executing command: $cmd")
        }
    }

    def logLine(String source, String line) {
        if (logOutput) {
            println("$source: $line")
        }
    }

    boolean isSuccess() {
        process.exitValue() == 0
    }

    boolean isTimeout() {
        process.exitValue() == 143
    }
}
