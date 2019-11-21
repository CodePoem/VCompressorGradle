package com.vdreamers.vcompressor.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vdreamers.vcompressor.compressor.CompressorFactory
import com.vdreamers.vcompressor.constants.CompressorConstants
import com.vdreamers.vcompressor.utils.FileUtils
import com.vdreamers.vcompressor.utils.LogUtils
import groovy.json.JsonOutput
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport.closeWithWarning
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files


class CompressTask : DefaultTask() {

    val compressConfig: CompressConfig
    private var logUtils: LogUtils? = null
    var resultInfo = CompressResultInfo()
    var sizeDirList: MutableList<String> = mutableListOf("greater500KB", "200~500KB", "100~200KB", "50~100KB", "20~50KB", "less20KB")

    init {
        description = "compressImageTask"
        group = "compressImage"
        compressConfig = project.extensions.getByName(CompressorConstants.EXT_OPT) as CompressConfig
    }

    @TaskAction
    fun run() {
        // 初始化日志工具
        logUtils = LogUtils.getInstance(project.getProject())
        logUtils?.i("ImgCompressTask run")
        // 非rootProject 抛异常
        require(project == project.rootProject) { "compress-plugin must works on project level gradle" }

        var imgDirectories = getSourcesDirs(project)
        var compressedList = getCompressedInfo()
        var unCompressFileList = getUnCompressFileList(imgDirectories, compressedList)

        resultInfo = CompressorFactory.getCompressor(compressConfig.compressWay).compress(project, unCompressFileList, compressConfig)

        copyToDebugPath(unCompressFileList)
        updateCompressInfoList(unCompressFileList, compressedList)

        logUtils?.i("Task finish, compressed:${resultInfo.compressedSize} files  skip:${resultInfo.skipCount} Files  before total size: ${FileUtils.formatFileSize(resultInfo.beforeSize)}" +
                " after total size: ${FileUtils.formatFileSize(resultInfo.afterSize)} save size: ${FileUtils.formatFileSize(resultInfo.beforeSize - resultInfo.afterSize)}  ")
    }

    /**
     * 获取所有的资源目录
     */
    fun getSourcesDirs(rootProject: Project): List<File> {
        var dirs: MutableList<File> = mutableListOf()

        rootProject.allprojects {
            rootProject.apply {
                // 仅对两种module做处理 App/Library
                logUtils?.i("ImgCompressTask deal ${project.name}")
                if (project.plugins.hasPlugin(AppPlugin::class.java)) {
                    dirs.addAll(getSourcesDirsWithVariant(((project.extensions.getByName("android") as AppExtension).applicationVariants) as DomainObjectSet<BaseVariant>))
                } else if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
                    dirs.addAll(getSourcesDirsWithVariant(((project.extensions.getByName("android") as LibraryExtension).libraryVariants) as DomainObjectSet<BaseVariant>))
                } else {
                    logUtils?.i("ignore project:" + project.name)
                }
            }
        }
        return dirs
    }

    /**
     * 根据当前module的variant获取所有打包方式的资源目录
     */
    fun getSourcesDirsWithVariant(collection: DomainObjectSet<BaseVariant>): MutableList<File> {
        var imgDirectories: MutableList<File> = mutableListOf()
        collection.all {
            logUtils?.i("-------- variant: $this.name --------")
            this.sourceSets?.forEach sourceSets@{ sourceSet ->
                logUtils?.i("sourceSets.${sourceSet.name} -->")
                if (sourceSet.resDirectories.isEmpty()) {
                    return@sourceSets
                }
                sourceSet.resDirectories.forEach resDirectories@{ res ->
                    if (res.exists()) {
                        logUtils?.i("${res.name}.directories:")
                        if (res.listFiles() == null) {
                            return@resDirectories
                        }
                        res.listFiles()?.forEach { file ->
                            if (file.isDirectory) {
                                if (file.name.startsWith("drawable") || file.name.startsWith("mipmap")) {
                                    if (!imgDirectories.contains(file)) {
                                        logUtils?.i("add dir $file")
                                        imgDirectories.add(file)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return imgDirectories
    }

    /**
     * 获取之前压缩文件信息
     */
    fun getCompressedInfo(): MutableList<CompressInfo> {
        // 读取原先已压缩过的文件,如果压缩过则不再压缩
        var compressedList = ArrayList<CompressInfo>()
        var compressedListFile = File("${project.projectDir}/image-compressed-info.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        } else {
            try {
                // 将已压缩过的文件json解析-->list
                val fis = FileInputStream(compressedListFile)
                val json = BufferedReader(InputStreamReader(fis, "utf-8"))
                val list = Gson().fromJson<ArrayList<CompressInfo>>(json, object : TypeToken<ArrayList<CompressInfo>>() {
                }.getType())
                if (list is ArrayList<*>) {
                    compressedList = list
                } else {
                    logUtils?.i("compressed-resource.json is invalid, ignore")
                }
            } catch (e: Exception) {
                logUtils?.i("compressed-resource.json is invalid, ignore")
            }
        }
        logUtils?.i("getCompressedInfo size=${compressedList.size}")
        return compressedList
    }

    /**
     * 获取待压缩的文件,过滤白名单目录及文件,过滤文件大小
     */
    fun getUnCompressFileList(imgDirectories: List<File>, compressedList: List<CompressInfo>): MutableList<CompressInfo> {
        var unCompressFileList: MutableList<CompressInfo> = mutableListOf()

        imgDirectories.forEach dirFlag@{ dir ->
            if (dir.listFiles() == null) {
                return@dirFlag
            }
            dir.listFiles()?.forEach fileFlag@{ file ->
                val fileName = file.name
                // 过滤白名单文件
                if (!compressConfig.whiteFiles.isEmpty()) {
                    compressConfig.whiteFiles.forEach { s ->
                        if (fileName.equals(s)) {
                            logUtils?.i("ignore whiteFiles >> " + file.absolutePath)
                            return@fileFlag
                        }
                    }
                }
                val newMd5 = FileUtils.generateMD5(file)
                // 过滤已压缩文件
                compressedList.forEach { compressInfo ->
                    // md5校验
                    if (compressInfo.md5.equals(newMd5)) {
                        logUtils?.i("ignore compressed >> " + file.absolutePath)
                        return@fileFlag
                    }
                }
                // 过滤非jpg或png图片
                if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png")) {
                    return@fileFlag
                }
                // .9图过滤
                if (fileName.contains(".9")) {
                    return@fileFlag
                }
                // 过滤文件大小
                if (getPicSize(file) < compressConfig.minSize) {
                    logUtils?.i("ignore size less than minSize  >> " + file.absolutePath)
                    return@fileFlag
                }
                unCompressFileList.add(CompressInfo(-1, -1, "", file.absolutePath, getOutputPath(file), newMd5))
                logUtils?.i("add file  outputPath >> ${getOutputPath(file)}")
            }
        }
        return unCompressFileList
    }

    /**
     * 获取图片大小,单位kb
     */
    fun getPicSize(file: File): Int {
        val fis = FileInputStream(file)
        val beforeSize: Int
        beforeSize = fis.available()
        fis.close()
        return beforeSize / 1024
    }

    /**
     * 根据配置确定输出路径
     */
    fun getOutputPath(originImg: File): String {
        if (compressConfig.debugMode) {
            var debugDir = File("${project.projectDir}/ImageCompressDebug")
            if (!debugDir.exists()) {
                debugDir.mkdir()
                sizeDirList.forEach { sizeDir ->
                    val sizePath = File("${project.projectDir}/ImageCompressTest/${sizeDir}")
                    if (!sizePath.exists()) sizePath.mkdir()
                }
            }
            var outPutPath = originImg.absolutePath
            val fis = FileInputStream(originImg)
            val beforeSize: Int
            beforeSize = fis.available()
            val originName = originImg.getName()
            val typeIndex = originName.indexOf(".")
            val testName = originName.substring(0, typeIndex) + "(test)" + originName.substring(typeIndex, originName.length)
            if (beforeSize < 1024 * 20) {
                outPutPath = "${project.projectDir}/ImageCompressTest/${sizeDirList[5]}/${testName}"
            } else if (beforeSize < 1024 * 50) {
                outPutPath = "${project.projectDir}/ImageCompressTest/${sizeDirList[4]}/${testName}"
            } else if (beforeSize < 1024 * 100) {
                outPutPath = "${project.projectDir}/ImageCompressTest/${sizeDirList[3]}/${testName}"
            } else if (beforeSize < 1024 * 200) {
                outPutPath = "${project.projectDir}/ImageCompressTest/${sizeDirList[2]}/${testName}"
            } else if (beforeSize < 1024 * 500) {
                outPutPath = "${project.projectDir}/ImageCompressTest/${sizeDirList[1]}/${testName}"
            } else {
                outPutPath = "${project.projectDir}/ImageCompressTest/${sizeDirList[0]}/${testName}"
            }
            return outPutPath

        } else {
            return originImg.absolutePath
        }
    }

    /**
     * 复制原文件到debug目录,便于比对
     */
    fun copyToDebugPath(newCompressedList: List<CompressInfo>) {
        if (!compressConfig.debugMode) {
            return
        }
        newCompressedList.forEach { compressInfo ->
            var origin = File(compressInfo.path)
            var debugPathName = File(compressInfo.outputPath).parent + "/" + origin.getName()
            var copyFile = File(debugPathName)
            if (copyFile.exists()) {
                copyFile.delete()
            }
            logUtils?.i("copyToDebugPath >>" + debugPathName)
            try {
                Files.copy(origin.toPath(), copyFile.toPath())
            } catch (e: Exception) {
                logUtils?.i("copyToTestPath" + e.printStackTrace())
            }
        }
    }

    /**
     * 更新已压缩信息
     */
    fun updateCompressInfoList(newCompressedList: List<CompressInfo>, compressedList: MutableList<CompressInfo>) {
        // 脱敏
        var projectDir = project.projectDir.absolutePath
        newCompressedList.forEach { compressInfo ->
            compressInfo.path = compressInfo.path.substring(projectDir.length, compressInfo.path.length)
            compressInfo.outputPath = compressInfo.outputPath.substring(projectDir.length, compressInfo.outputPath.length)
        }
        newCompressedList.forEach newCompressedList@{ newCompressInfo ->
            newCompressedList.forEach { compressInfo ->
                if (newCompressInfo.md5.equals(compressInfo.md5)) {
                    val index = compressedList.indexOf(compressInfo)
                    if (index >= 0) {
                        compressedList[index] = newCompressInfo
                    } else {
                        compressedList.add(0, newCompressInfo)
                    }
                    return@newCompressedList
                }
            }
        }
        var jsonOutput = JsonOutput()
        var json = JsonOutput.toJson(compressedList)

        var compressedListFile = File("${project.projectDir}/image-compressed-info.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        }
        var writer: OutputStreamWriter? = null

        try {
            val out = FileOutputStream(compressedListFile)
            if ("UTF-16BE".equals(Charset.forName("utf-8").name())) {
                out.write(-2);
                out.write(-1);
            } else if ("UTF-16LE".equals(Charset.forName("utf-8").name())) {
                out.write(-1);
                out.write(-2);
            }
            writer = OutputStreamWriter(out, "utf-8")
            writer.write(JsonOutput.prettyPrint(json))
            writer.flush()
            val temp = writer
            writer = null
            temp.close()
        } finally {
            closeWithWarning(writer)
        }
    }
}