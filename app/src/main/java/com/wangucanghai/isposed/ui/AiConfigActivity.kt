package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.ai.AiClient
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI service provider config: pull models & test model.
 *
 * Copyright (C) 2026 万古沧海
 */
class AiConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "AI 服务商配置", onBack = { finish() }) {
                    AiConfigContent()
                }
            }
        }
    }
}

@Composable
private fun AiConfigContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    val client = remember { AiClient(context) }

    var baseUrl by remember { mutableStateOf(prefs.aiBaseUrl) }
    var apiKey by remember { mutableStateOf(prefs.aiApiKey) }
    var model by remember { mutableStateOf(prefs.aiModel) }
    var temp by remember { mutableFloatStateOf(prefs.aiTemperature) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    SectionCard {
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        SpacerH(10)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        SpacerH(10)
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("模型") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        SpacerH(10)
        Text("温度：${"%.2f".format(temp)}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = temp,
            onValueChange = { temp = it },
            valueRange = 0f..2f,
            steps = 8,
        )
    }
    SpacerH(16)
    Button(
        onClick = {
            prefs.aiBaseUrl = baseUrl.trim()
            prefs.aiApiKey = apiKey.trim()
            prefs.aiModel = model.trim()
            prefs.aiTemperature = temp
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            Logger.i("AI config saved")
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("保存")
    }
    SpacerH(10)
    OutlinedButton(
        onClick = {
            prefs.aiBaseUrl = baseUrl.trim()
            prefs.aiApiKey = apiKey.trim()
            result = "拉取模型中…"
            busy = true
            scope.launch {
                val models = withContext(Dispatchers.IO) { runCatching { client.listModels() }.getOrDefault(emptyList()) }
                result = if (models.isEmpty()) "未获取到模型（检查 Base URL / Key）"
                else "模型列表：\n" + models.joinToString("\n")
                if (model.isBlank()) model = models.firstOrNull() ?: ""
                busy = false
            }
        },
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("拉取模型")
    }
    SpacerH(10)
    OutlinedButton(
        onClick = {
            prefs.aiBaseUrl = baseUrl.trim()
            prefs.aiApiKey = apiKey.trim()
            prefs.aiModel = model.trim()
            val m = model.trim()
            if (m.isEmpty()) { Toast.makeText(context, "请填写模型名", Toast.LENGTH_SHORT).show(); return@OutlinedButton }
            result = "测试中…"
            busy = true
            scope.launch {
                val r = withContext(Dispatchers.IO) { runCatching { client.testModel(m) } }
                result = r.fold(
                    onSuccess = { "测试成功：$it" },
                    onFailure = { "测试失败：${it.message}" }
                )
                busy = false
            }
        },
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("测试模型")
    }
    if (result.isNotEmpty()) {
        SpacerH(16)
        SectionCard {
            Text(
                result,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
    SpacerH(28)
}
