package com.github.mrduguo.gradle.buildscript.utils

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class TimeHelper {

    public static def waitForActionInMinutes(def action, def successNotice = null, long timeoutInMinutes = 600) {
        long startTimestamp = System.currentTimeMillis()
        while (true) {
            def result = action()
            if (result != null) {
                if (successNotice) {
                    println "${successNotice(result)} in ${((System.currentTimeMillis() - startTimestamp) / 1000 / 60).toInteger()} minutes"
                }
                return result
            } else {
                if (startTimestamp + timeoutInMinutes * 60 * 1000 < System.currentTimeMillis()) {
                    throw new RuntimeException("timeout after $timeoutInMinutes minutes")
                } else {
                    print ", waiting for 1 minutes ...\n"
                    Thread.sleep(1000 * 60)
                }
            }
        }
    }

    public static boolean isInDublinOfficeHour(def currentTime = new Date()) {
        def dublinTime = getDublinLocalTime(currentTime)
        if (
        dublinTime.getDayOfWeek() >= 1 && dublinTime.getDayOfWeek() <= 5 &&
                dublinTime.getHourOfDay() >= 8 && dublinTime.getHourOfDay() <= 19
        ) {
            true
        } else {
            false
        }
    }

    public static DateTime getDublinLocalTime(def currentTime = new Date()) {
        new DateTime(currentTime).withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone('Europe/Dublin')))
    }
}
