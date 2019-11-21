package com.vdreamers.vcompressor.plugin

import com.vdreamers.vcompressor.constants.CompressorConstants
import org.gradle.api.Plugin
import org.gradle.api.Project

class CompressorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("CompressorPlugin  call " + project.name + "  gradle:" + project.gradle.toString() + " " + (project == project.rootProject))
        // 非rootProject 抛异常
        require(project == project.rootProject) { "compress-plugin must works on project level gradle" }
        project.run {
            extensions.create(CompressorConstants.EXT_OPT, CompressConfig::class.java)
            // 继承CompressTask 替换现有任务
            val args: Map<String, Any> = mapOf("type" to CompressTask::class.java, "overwrite" to true)
            task(args, "compressImage")
        }
    }
}