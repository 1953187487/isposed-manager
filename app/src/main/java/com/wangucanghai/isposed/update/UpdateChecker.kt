package com.wangucanghai.isposed.update

import android.content.Context
import com.google.gson.JsonParser
import com.wangucanghai.isposed.core.Constants
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Checks for a new version against the GitHub releases API and compares with
 * the currently installed versionCode/versionName.
 *
 * Copyright (C) 2026 万古沧海
 */
class UpdateChecker(private val context: Context) {

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val latestVersionCode: Long,
        val currentVersion: String,
        val currentVersionCode: Long,
        val notes: String,
        val downloadUrl: String,
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        val curCode = versionCode()
        val curName = versionName()
        try {
            val req = Request.Builder()
                .url(Constants.UPDATE_API_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext UpdateInfo(false, curName, curCode, curName, curCode, "", "")
                }
                val body = resp.body?.string() ?: ""
                val obj = JsonParser.parseString(body).asJsonObject
                val tag = obj.get("tag_name").asString
                val latestCode = tag.trimStart('v').substringBefore('-').split('.').let {
                    it.fold(0L) { acc, s -> acc * 1000 + (s.toLongOrNull() ?: 0) }
                }
                val notes = obj.get("body").asString
                val assets = obj.getAsJsonArray("assets")
                val url = if (assets.size() > 0)
                    assets[0].asJsonObject.get("browser_download_url").asString
                else obj.get("html_url").asString
                val hasUpdate = compareVersions(tag.trimStart('v'), curName)
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = tag,
                    latestVersionCode = latestCode,
                    currentVersion = curName,
                    currentVersionCode = curCode,
                    notes = notes,
                    downloadUrl = url,
                )
            }
        } catch (t: Throwable) {
            Logger.e("update check failed: ${t.message}")
            UpdateInfo(false, curName, curCode, curName, curCode, "", "")
        }
    }

    private fun compareVersions(latest: String, current: String): Boolean {
        val l = latest.trimStart('v').split('.').mapNotNull { it.toIntOrNull() }
        val c = current.split('.').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun versionCode(): Long = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode
        else @Suppress("DEPRECATION") pi.versionCode.toLong()
    } catch (_: Exception) { 0 }

    private fun versionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    } catch (_: Exception) { "0.0.0" }
}
