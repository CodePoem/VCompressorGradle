package com.vdreamers.vcompressor.plugin

class CompressInfo(var preSize: Long, var compressedSize: Long, var ratio: String, var path: String, var outputPath: String, var md5: String) {

    fun update(preSize: Long, compressedSize: Long, md5: String) {
        this.preSize = preSize
        this.compressedSize = compressedSize
        this.md5 = md5
        ratio = (compressedSize * 1.0F / preSize * 100).toString() + "%"
    }
}