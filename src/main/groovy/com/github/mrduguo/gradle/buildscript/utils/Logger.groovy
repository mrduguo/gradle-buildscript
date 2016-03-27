package com.github.mrduguo.gradle.buildscript.utils

import org.gradle.api.logging.LogLevel

class Logger {

    public static void info(String msg) {
        log(LogLevel.LIFECYCLE, msg)
    }

    public static void warn(String msg) {
        log(LogLevel.WARN, msg)
    }

    public static void error(String msg) {
        log(LogLevel.ERROR, msg)
    }

    public static void debug(String msg, def data = null) {
        if (ProjectHelper.project) {
            if (data) {
                msg = "$msg: $data"
            }
            log(LogLevel.DEBUG, msg)
        }
    }

    public static void throwException(String msg, Exception ex = null) {
        if (msg) {
            log(LogLevel.ERROR, msg)
        } else {
            log(LogLevel.ERROR, "${ex.getClass().name} $ex.message")
        }
        if (!ex) {
            ex = new RuntimeException(msg)
        }
        ex.printStackTrace()
        throw ex
    }

    private static void log(LogLevel logLevel, String msg) {
        msg = "${new Date().format('yy-MM-dd HH:mm:ss')} ${logLevel == LogLevel.LIFECYCLE ? '' : logLevel.name()} $msg"
        if (ProjectHelper.project) {
            ProjectHelper.project.logger.log(logLevel, msg)
        } else {
            println(msg)
        }
    }
}
