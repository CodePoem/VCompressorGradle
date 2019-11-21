package com.vdreamers.vcompressor.compressor

import com.vdreamers.vcompressor.plugin.CompressConfig
import com.vdreamers.vcompressor.plugin.CompressInfo
import com.vdreamers.vcompressor.plugin.CompressResultInfo
import org.gradle.api.Project

interface ICompressor {

    fun compress(rootProject: Project, unCompressFileList: List<CompressInfo>, config: CompressConfig): CompressResultInfo
}