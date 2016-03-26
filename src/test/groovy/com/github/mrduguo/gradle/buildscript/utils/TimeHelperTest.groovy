package com.github.mrduguo.gradle.buildscript.utils

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


class TimeHelperTest extends spock.lang.Specification {
    def "time is right with dublin timezone"() {
        expect:
        DateTime utcTime = ISODateTimeFormat.dateTimeParser().parseDateTime(time)
        TimeHelper.isInDublinOfficeHour(utcTime) == isIn

        where:
        time                   | isIn
        '2015-10-05T14:56:53Z' | true
        '2015-10-06T14:56:53Z' | true
        '2015-10-07T14:56:53Z' | true
        '2015-10-08T14:56:53Z' | true
        '2015-10-09T14:56:53Z' | true
        '2015-10-10T14:56:53Z' | false
        '2015-10-11T14:56:53Z' | false
        '2015-10-12T14:56:53Z' | true


        '2015-10-09T06:00:00Z' | false
        '2015-10-09T07:00:00Z' | true
        '2015-10-09T18:00:00Z' | true
        '2015-10-09T19:00:00Z' | false

        '2015-12-07T07:00:00Z' | false
        '2015-12-07T08:00:00Z' | true
        '2015-12-07T19:00:00Z' | true
        '2015-12-07T19:59:59Z' | true
        '2015-12-07T20:00:00Z' | false
    }
}
