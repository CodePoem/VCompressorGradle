package com.vdreamers.vcompressor.utils

import org.apache.tools.ant.taskdefs.condition.Os
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.closeWithWarning
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream


object PngquantUtil {

    private val name = "pngquant"

    fun copyPngquant2BuildFolder(project: Project) {
        var pngquantDir = getPngquantDirectory(project)
        if (!pngquantDir.exists()) {
            pngquantDir.mkdirs()
        }
        var pngFile = File(getPngquantFilePath(project))
        if (!pngFile.exists()) {

            var os: FileOutputStream? = FileOutputStream(pngFile)
            try {
                var inputStream = PngquantUtil::class.java.getResourceAsStream("/$name/${getFilename()}")
                val result = os?.write(inputStream.readBytes())
                os?.flush()
                val temp = os
                os = null
                temp?.close()
            } finally {
                closeWithWarning(os)
            }
        }
        pngFile.setExecutable(true, false)
    }

    /**
     * .../build/pngquant
     */
    fun getPngquantDirectoryPath(project: Project): String {
        return project.buildDir.absolutePath + File.separator + "$name"
    }

    /**
     * .../build/pngquant
     */
    fun getPngquantDirectory(project: Project): File {
        return File(getPngquantDirectoryPath(project))
    }

    /**
     * .../build/pngquant/{pngquant/pngquant-mac/pngquant.exe}.
     */
    fun getPngquantFilePath(project: Project): String {
        return getPngquantDirectoryPath(project) + File.separator + getFilename()
    }

    fun getFilename(): String {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return "${name}.exe"
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            return "${name}-mac"
        } else {
            return name
        }
    }
}