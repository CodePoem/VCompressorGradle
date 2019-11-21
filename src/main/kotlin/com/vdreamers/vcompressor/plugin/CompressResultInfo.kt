package com.vdreamers.vcompressor.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CompressResultInfo {
    var beforeSize: Long = 0
    var afterSize: Long = 0
    var compressedSize: Int = 0
    var skipCount: Int = 0
}