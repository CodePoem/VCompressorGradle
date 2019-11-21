package com.vdreamers.vcompressor.utils

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.text.DecimalFormat

object FileUtils {

    fun generateMD5(file: File): String {
        val digest: MessageDigest = MessageDigest.getInstance("MD5")
        val ins = file.inputStream()
        var read: Int
        val buffer = ByteArray(8192)
        read = ins.read(buffer)
        while (read > 0) {
            read = ins.read(buffer)
            digest.update(buffer, 0, read)
        }
        val md5sum: ByteArray = digest.digest()
        val bigInt = BigInteger(1, md5sum)
        return bigInt.toString(16).padStart(32, '0')
    }

    fun formatFileSize(fileSize: Long): String {
        val df = DecimalFormat("#.00")
        if (fileSize == 0L) {
            return "0B"
        }

        if (fileSize < 1024) {
            return df.format(fileSize.toDouble()) + "B"
        } else if (fileSize < 1048576) {
            return df.format(fileSize.toDouble() / 1024) + "KB"
        } else if (fileSize < 1073741824) {
            return df.format(fileSize.toDouble() / 1048576) + "MB"
        } else {
            return df.format(fileSize.toDouble() / 1073741824) + "GB"
        }
    }
}