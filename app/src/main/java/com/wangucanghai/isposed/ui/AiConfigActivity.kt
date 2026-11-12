package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ai.AiClient
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI service provider config: pull models & test model.
 *
 * Copyright (C) 2026 万古沧海
 */
class AiConfigActivity : AppCompatActivity() {

    private val prefs by lazy { AppPrefs(this) }
    private val client by lazy { AiClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_config)

        val baseUrl = findViewById<TextInputEditText>(R.id.input_base_url)
        val apiKey = findViewById<TextInputEditText>(R.id.input_api_key)
        val model = findViewById<TextInputEditText>(R.id.input_model)
        val temp = findViewById<Slider>(R.id.slider_temp)
        val result = findViewById<TextView>(R.id.result)

        baseUrl.setText(prefs.aiBaseUrl)
        apiKey.setText(prefs.aiApiKey)
        model.setText(prefs.aiModel)
        temp.value = prefs.aiTemperature

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            prefs.aiBaseUrl = baseUrl.text.toString().trim()
            prefs.aiApiKey = apiKey.text.toString().trim()
            prefs.aiModel = model.text.toString().trim()
            prefs.aiTemperature = temp.value
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            Logger.i("AI config saved")
        }

        findViewById<MaterialButton>(R.id.btn_pull).setOnClickListener {
            prefs.aiBaseUrl = baseUrl.text.toString().trim()
            prefs.aiApiKey = apiKey.text.toString().trim()
            result.text = "拉取模型中…"
            CoroutineScope(Dispatchers.Main).launch {
                val models = withContext(Dispatchers.IO) { runCatching { client.listModels() }.getOrDefault(emptyList()) }
                if (models.isEmpty()) result.text = "未获取到模型（检查 Base URL / Key）"
                else {
                    result.text = "模型列表：\n" + models.joinToString("\n")
                    if (model.text.isNullOrBlank()) model.setText(models.firstOrNull() ?: "")
                }
            }
        }

        findViewById<MaterialButton>(R.id.btn_test).setOnClickListener {
            prefs.aiBaseUrl = baseUrl.text.toString().trim()
            prefs.aiApiKey = apiKey.text.toString().trim()
            prefs.aiModel = model.text.toString().trim()
            val m = model.text.toString().trim()
            if (m.isEmpty()) { Toast.makeText(this, "请填写模型名", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            result.text = "测试中…"
            CoroutineScope(Dispatchers.Main).launch {
                val r = withContext(Dispatchers.IO) { runCatching { client.testModel(m) } }
                result.text = r.fold(
                    onSuccess = { "测试成功：$it" },
                    onFailure = { "测试失败：${it.message}" }
                )
            }
        }
    }
}
