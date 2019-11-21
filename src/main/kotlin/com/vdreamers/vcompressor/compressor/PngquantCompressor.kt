package com.vdreamers.vcompressor.compressor

import com.vdreamers.vcompressor.plugin.CompressConfig
import com.vdreamers.vcompressor.plugin.CompressInfo
import com.vdreamers.vcompressor.plugin.CompressResultInfo
import com.vdreamers.vcompressor.utils.FileUtils
import com.vdreamers.vcompressor.utils.LogUtils
import com.vdreamers.vcompressor.utils.PngquantUtil
import org.gradle.api.Project
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files

class PngquantCompressor : ICompressor {

    var logUtils: LogUtils? = null

    override fun compress(rootProject: Project, unCompressFileList: List<CompressInfo>, config: CompressConfig): CompressResultInfo {
        var compressResultInfo: CompressResultInfo = CompressResultInfo()
        logUtils = LogUtils.getInstance(rootProject)
        var beforeTotalSize = 0L
        var afterTotalSize = 0L
        var skipCount = 0

        PngquantUtil.copyPngquant2BuildFolder(rootProject)
        var pngquant = PngquantUtil.getPngquantFilePath(rootProject)

        unCompressFileList.forEach { compressInfo ->
            var originFile = File(compressInfo.path)
            var type = originFile.getAbsolutePath().substring(originFile.getAbsolutePath().indexOf("."))
            var suffix: String = if (config.debugMode) "(test)" else ""
            if (type.equals(".png")) {
                suffix = if (config.debugMode) "(test).png" else ".png"
            }

            var originalSize = originFile.length()
            var process = ProcessBuilder(pngquant, "-v", "--force", "--skip-if-larger",
                    "--speed=1", "--ext=${suffix}", compressInfo.path).redirectErrorStream(true).start();

            var br = BufferedReader(InputStreamReader(process.getInputStream()))
            var error = StringBuilder()
            var line = br.readLine()
            while (null != line) {
                error.append(line)
                line = br.readLine()
            }
            var exitCode = process.waitFor()
            if (exitCode == 0) {
                if (config.debugMode) {
                    //复制test文件到测试目录
                    var testName = File(compressInfo.outputPath).name
                    var testPath = File(compressInfo.path).parent + "/" + testName
                    copyToDebugPath(testPath, compressInfo.outputPath)
                    var testFile = File(testPath)
                    if (testFile.exists()) {
                        testFile.delete()
                    }
                }


                var optimizedSize = File(compressInfo.outputPath).length()
                var rate = 1.0f * (originalSize - optimizedSize) / originalSize * 100
                compressInfo.update(originalSize, optimizedSize, FileUtils.generateMD5(File(compressInfo.outputPath)))
                logUtils?.i("Succeed! ${FileUtils.formatFileSize(originalSize)}-->${FileUtils.formatFileSize(optimizedSize)}, ${rate}% saved! ${compressInfo.outputPath}")
                beforeTotalSize += originalSize
                afterTotalSize += optimizedSize
            } else if (exitCode == 98) {
                logUtils?.w("Skipped! ${compressInfo.path}")
                skipCount++
            } else {
                logUtils?.e("Failed! ${compressInfo.path}")
                skipCount++
            }
        }

        return compressResultInfo
    }

    /**
     * 复制原文件到测试目录,便于比对
     */
    fun copyToDebugPath(orginTestPath: String, outputPath: String) {
        var origin = File(orginTestPath)
        var copyFile = File(outputPath)
        if (copyFile.exists()) {
            copyFile.delete()
        }
        try {
            Files.copy(origin.toPath(), copyFile.toPath())
        } catch (e: Exception) {
            logUtils?.i("copyToTestPath" + e.printStackTrace())
        }
    }

}