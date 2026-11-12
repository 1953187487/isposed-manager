package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ai.AiClient
import com.wangucanghai.isposed.ai.ModuleConverter
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI module conversion: converts an Isposed/Xposed module to an adapter for this manager.
 *
 * Copyright (C) 2026 万古沧海
 */
class ConvertActivity : AppCompatActivity() {

    private val prefs by lazy { AppPrefs(this) }
    private val client by lazy { AiClient(this) }
    private var pickedPkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert)
        val picked = findViewById<TextView>(R.id.picked)
        val result = findViewById<TextView>(R.id.result)

        findViewById<MaterialButton>(R.id.btn_pick).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val modules = withContext(Dispatchers.IO) { ModuleRepository(this@ConvertActivity).loadModules() }
                if (modules.isEmpty()) {
                    Toast.makeText(this@ConvertActivity, "未发现可转换的模块", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val names = modules.map { it.label + "  (" + it.packageName + ")" }
                AlertDialog.Builder(this@ConvertActivity)
                    .setTitle("选择模块")
                    .setItems(names.toTypedArray()) { _, which ->
                        val m = modules[which]
                        pickedPkg = m.packageName
                        picked.text = m.label + " · v" + m.versionName
                    }
                    .show()
            }
        }

        findViewById<MaterialButton>(R.id.btn_start).setOnClickListener {
            val pkg = pickedPkg ?: run {
                Toast.makeText(this, "请先选择模块", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (!client.isConfigured()) {
                Toast.makeText(this, "请先配置 AI 服务商 API", Toast.LENGTH_LONG).show()
                startActivity(android.content.Intent(this, AiConfigActivity::class.java))
                return@setOnClickListener
            }
            result.text = "分析并转换中…"
            CoroutineScope(Dispatchers.Main).launch {
                val desc = withContext(Dispatchers.IO) { ModuleConverter(this@ConvertActivity).analyze(pkg) }
                if (desc == null) {
                    result.text = "无法分析该模块"
                    return@launch
                }
                val adapterJson = withContext(Dispatchers.IO) { ModuleConverter(this@ConvertActivity).toAdapter(desc) }
                result.text = "模块分析完成：\n入口：${desc.entryPoints}\n\n适配器描述：\n$adapterJson"
                Logger.i("Converted ${desc.packageName}")
            }
        }
    }
}
