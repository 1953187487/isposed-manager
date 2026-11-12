package com.wangucanghai.isposed.ai

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.wangucanghai.isposed.util.Logger

/**
 * AI module converter: analyzes an existing Xposed/LSPosed module APK and
 * produces an adapter so it runs under the manager's root-free runtime.
 *
 * Because our runtime hosts modules through the standard `de.robv.android.xposed`
 * API (LSPosed-compatible), conversion is primarily an *analysis + adaptation*
 * step: we inspect the APK's manifest meta-data to locate the entry points, then
 * hand the descriptor to the AI to produce an adapter module (dex + manifest).
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleConverter(private val context: Context) {

    data class ModuleDescriptor(
        val packageName: String,
        val label: String,
        val versionName: String,
        val entryPoints: List<String>,
        val description: String?,
        val usesResources: Boolean,
        val minVersion: Int,
    )

    private val pm: PackageManager = context.packageManager

    /** Analyze an installed Xposed module APK for conversion. */
    fun analyze(packageName: String): ModuleDescriptor? {
        return try {
        val info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val meta = info.metaData
        if (meta == null || !meta.getBoolean("xposedmodule", false)) return null
        val entry = meta.getString("xposedmain") ?: meta.getString("xposedentry")
        ModuleDescriptor(
            packageName = packageName,
            label = pm.getApplicationLabel(info).toString(),
            versionName = version(info),
            entryPoints = listOfNotNull(entry),
            description = meta.getString("xposeddescription"),
            usesResources = meta.getBoolean("xposedmodule", false) || meta.containsKey("xposedminversion"),
            minVersion = meta.getInt("xposedminversion", 0),
        )
    } catch (t: Throwable) {
        Logger.e("analyze $packageName: ${t.message}")
        null
    }
    }

    private fun version(info: ApplicationInfo): String = try {
        val pi = pm.getPackageInfo(info.packageName, 0)
        pi.versionName ?: ""
    } catch (_: Exception) { "" }

    /**
     * Produce an adapter definition that hosts the original module under the
     * manager runtime. Returns generated module descriptor JSON for the AI
     * build step.
     */
    fun toAdapter(desc: ModuleDescriptor): String {
        val json = StringBuilder()
        json.appendLine("{")
        json.appendLine("  \"name\": \"${desc.label}\",")
        json.appendLine("  \"package\": \"${desc.packageName}.isposedadapter\",")
        json.appendLine("  \"entryPoints\": ${gsonList(desc.entryPoints)},")
        json.appendLine("  \"description\": \"${desc.description ?: ""}\",")
        json.appendLine("  \"sourceModule\": \"${desc.packageName}\",")
        json.appendLine("  \"license\": \"Original module license applies; adapter GPL-3.0\"")
        json.append("}")
        return json.toString()
    }

    private fun gsonList(list: List<String>): String =
        "[" + list.joinToString(",") { "\"$it\"" } + "]"
}
