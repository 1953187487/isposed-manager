package com.wangucanghai.isposed.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.ai.AiClient
import com.wangucanghai.isposed.ai.ModuleCodeGen
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI direct development: generate a complete LSPosed module from a prompt.
 *
 * Copyright (C) 2026 万古沧海
 */
class DevActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "AI 直接开发", onBack = { finish() }) {
                    DevContent()
                }
            }
        }
    }
}

@Composable
private fun DevContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { AiClient(context) }
    var prompt by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = prompt,
        onValueChange = { prompt = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("描述要生成的模块功能，例如：隐藏状态栏图标、修改音量条位置…") },
        minLines = 3,
        shape = RoundedCornerShape(14.dp),
    )
    SpacerH(12)
    Button(
        onClick = {
            val text = prompt.trim()
            if (text.isEmpty()) { Toast.makeText(context, "请输入模块功能描述", Toast.LENGTH_SHORT).show(); return@Button }
            if (!client.isConfigured()) {
                Toast.makeText(context, "请先配置 AI 服务商 API", Toast.LENGTH_LONG).show()
                context.startActivity(Intent(context, AiConfigActivity::class.java))
                return@Button
            }
            result = "AI 生成中…（可能需要几十秒）"
            busy = true
            scope.launch {
                val gen = ModuleCodeGen(context)
                val r = withContext(Dispatchers.IO) { gen.generate(text) }
                result = if (r.ok) {
                    val files = r.files.keys.joinToString("\n")
                    "生成成功，共 ${r.files.size} 个文件：\n$files\n\n" +
                        r.files.entries.joinToString("\n\n") { (k, v) -> "===== $k =====\n$v" }
                } else {
                    r.message
                }
                busy = false
                Logger.i("AI codegen: ok=${r.ok} files=${r.files.size}")
            }
        },
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
        Text("生成模块")
    }
    SpacerH(16)
    if (result.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = result,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
    }
    SpacerH(28)
}
