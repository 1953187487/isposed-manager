package com.wangucanghai.isposed.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.update.UpdateChecker
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Update checker: compares the installed version with the latest release.
 *
 * Copyright (C) 2026 万古沧海
 */
class UpdateActivity : AppCompatActivity() {

    private val prefs by lazy { AppPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)
        val status = findViewById<TextView>(R.id.status)
        val notes = findViewById<TextView>(R.id.notes)
        val btn = findViewById<MaterialButton>(R.id.btn_download)

        status.text = getString(R.string.update_current, versionName())
        CoroutineScope(Dispatchers.Main).launch {
            val info = withContext(Dispatchers.IO) { UpdateChecker(this@UpdateActivity).check() }
            prefs.lastCheckedVersion = info.latestVersion
            prefs.lastUpdateNotes = info.notes
            if (info.hasUpdate) {
                status.text = getString(R.string.update_found) + " · 当前 v${info.currentVersion} → 最新 ${info.latestVersion}"
                notes.text = info.notes.ifBlank { "无更新说明" }
                btn.visibility = android.view.View.VISIBLE
                btn.setOnClickListener {
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                    }
                }
            } else {
                status.text = getString(R.string.update_none) + " · 当前 v${info.currentVersion} / 最新 ${info.latestVersion}"
                notes.text = "已是最新版本。"
                btn.visibility = android.view.View.GONE
            }
            Logger.i("Update check: ${info.latestVersion} hasUpdate=${info.hasUpdate}")
        }
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) { "1.0.0" }
}
