package com.vdreamers.vcompressor.utils

import org.gradle.api.Project
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class LogUtils private constructor(project: Project) {

    private lateinit var file: File
    private lateinit var writer: Writer
    private var outPutLog: Boolean = false

    init {
        if (outPutLog) {
            file = File(project.projectDir.absolutePath + File.separator + LOG_FILE_NAME)
            PrintWriter(file).close()
        }
    }

    companion object {
        private const val LOG_FILE_NAME = "compress_image.log"
        private const val INFO = "info:  "
        private const val WARN = "warn:  "
        private const val ERROR = "error: "

        @Volatile
        private var instance: LogUtils? = null

        fun getInstance(project: Project) =
                instance ?: synchronized(this) {
                    instance ?: LogUtils(project).also { instance = it }
                }
    }

    private fun write(logLevel: String, msg: String) {
        if (!outPutLog) return
        writer = PrintWriter(BufferedWriter(OutputStreamWriter(
                FileOutputStream(file, true), "UTF-8")), true)
        try {
            writer.write(getDateTime() + "  " + logLevel)
            writer.write(msg + "\r\n")
            writer.write("----------------------------------------\r\n")
        } catch (e: Exception) {

        } finally {
            writer.close()
        }
    }

    private fun getDateTime(): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(Date())
    }

    fun i(msg: String) {
        write(INFO, msg)
        println(msg)
    }

    fun w(msg: String) {
        write(WARN, msg)
        println(msg)
    }

    fun e(msg: String) {
        write(ERROR, msg)
        println(msg)
    }

}