package com.wangucanghai.isposed.ai

import android.content.Context
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI module development: given a natural-language prompt, produce a complete,
 * buildable LSPosed-compatible module (manifest + entry source) via the AI.
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleCodeGen(private val context: Context) {

    data class GenResult(val ok: Boolean, val message: String, val files: Map<String, String>)

    private val client = AiClient(context)

    private val systemPrompt = """
        你是一个 Android LSPosed/Xposed 模块开发专家。用户会给你一个模块开发需求。
        你需要输出一个完整的、可编译的 LSPosed 模块工程（Java 或 Kotlin），文件结构如下：
        - AndroidManifest.xml : 声明 xposedmodule=true, xposeddescription, xposedminversion, xposedmain 指向入口类
        - src/Entry.java (或 .kt) : 实现 IXposedHookLoadPackage / IXposedHookInitPackageResources
        - build.gradle : 依赖 de.robv.android.xposed:api:82
        要求：
        1. 只输出 JSON，格式 {"files": {"AndroidManifest.xml": "...", "src/Entry.java": "...", "build.gradle": "..."}}
        2. 文件内容转义为 JSON 字符串（\n, \t, \"）。
        3. 代码必须使用标准 de.robv.android.xposed.* API，方法用 XposedHelpers.findAndHookMethod。
        4. 不要输出 JSON 之外的任何文字。
    """.trimIndent()

    suspend fun generate(requirement: String): GenResult = withContext(Dispatchers.IO) {
        try {
            val raw = client.chat(systemPrompt, requirement)
            val files = parseFiles(raw)
            if (files.isEmpty()) {
                GenResult(false, "AI 未返回有效文件结构：${raw.take(160)}", emptyMap())
            } else {
                GenResult(true, "生成 ${files.size} 个文件", files)
            }
        } catch (t: Throwable) {
            Logger.e("codegen: ${t.message}")
            GenResult(false, "生成失败：${t.message}", emptyMap())
        }
    }

    private fun parseFiles(raw: String): Map<String, String> {
        val jsonStr = raw.substringAfter('{').let { "{" + it.substringBeforeLast('}') + "}" }
        return try {
            val obj = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
            val files = obj.getAsJsonObject("files")
            files.entrySet().associate { (k, v) -> k to v.asString }
        } catch (_: Exception) {
            // try to locate a ```json block
            val block = Regex("```json\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL)
                .find(raw)?.groupValues?.get(1)
            if (block != null) {
                try {
                    val obj = com.google.gson.JsonParser.parseString(block).asJsonObject
                    obj.getAsJsonObject("files").entrySet().associate { (k, v) -> k to v.asString }
                } catch (_: Exception) { emptyMap() }
            } else emptyMap()
        }
    }
}
