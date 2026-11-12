package com.wangucanghai.isposed.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.wangucanghai.isposed.util.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * In-app update installer: downloads the release APK and hands it to the system
 * package installer via a FileProvider URI, so the update happens inside the app
 * instead of opening a browser.
 *
 * Copyright (C) 2026 万古沧海
 */
class UpdateInstaller(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Download the APK from [url] into the app cache dir. Returns the file or null. */
    fun download(url: String): File? {
        return try {
            val req = Request.Builder().url(url).build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Logger.e("update download failed: ${resp.code}")
                    return null
                }
                val body = resp.body ?: return null
                val out = File(context.cacheDir, "isposed_update.apk")
                out.outputStream().use { os ->
                    body.byteStream().use { it.copyTo(os) }
                }
                Logger.i("update downloaded: ${out.length()} bytes")
                out
            }
        } catch (t: Throwable) {
            Logger.e("update download error: ${t.message}")
            null
        }
    }

    /** Launch the system package installer for the downloaded APK. */
    fun install(apk: File): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Logger.e("update install intent failed: ${t.message}")
            false
        }
    }
}
