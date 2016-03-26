package com.github.mrduguo.gradle.buildscript.utils


class TextHelper {

    public static File replaceText(File file, String from, String to) {
        file.write(file.text.replace(from, to))
        file
    }
}
