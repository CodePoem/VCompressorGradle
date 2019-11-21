package com.vdreamers.vcompressor.plugin

import com.vdreamers.vcompressor.constants.CompressorConstants

class CompressConfig {
    var debugMode: Boolean = false
    var compressWay: String = CompressorConstants.COMPRESS_WAY_PNGQUANT
    var minSize = 0
    var whiteDirs: ArrayList<String> = ArrayList()
    var whiteFiles: ArrayList<String> = ArrayList()
}