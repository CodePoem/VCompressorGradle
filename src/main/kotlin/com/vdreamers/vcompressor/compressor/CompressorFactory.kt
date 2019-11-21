package com.vdreamers.vcompressor.compressor

import com.vdreamers.vcompressor.constants.CompressorConstants

class CompressorFactory {

    companion object {
        fun getCompressor(compressWay: String): ICompressor {
            return when (compressWay) {
                CompressorConstants.COMPRESS_WAY_PNGQUANT -> PngquantCompressor()
                else -> PngquantCompressor()
            }
        }
    }
}