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
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.wangucanghai.isposed.ai.ModuleConverter
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI module conversion: converts an Isposed/Xposed module to an adapter for this manager.
 *
 * Copyright (C) 2026 万古沧海
 */
class ConvertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "模块转换", onBack = { finish() }) {
                    ConvertContent()
                }
            }
        }
    }
}

private data class ModuleChoice(val label: String, val pkg: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    val client = remember { AiClient(context) }

    var modules by remember { mutableStateOf<List<ModuleChoice>>(emptyList()) }
    var picked by remember { mutableStateOf<ModuleChoice?>(null) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // Load modules once.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val list = withContext(Dispatchers.IO) { ModuleRepository(context).loadModules() }
        modules = list.map { ModuleChoice(it.label + "  (" + it.packageName + ")", it.packageName) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = picked?.label ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("选择模块") },
            placeholder = { Text("未选择") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (modules.isEmpty()) {
                DropdownMenuItem(text = { Text("未发现可转换的模块") }, onClick = { expanded = false })
            }
            modules.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.label) },
                    onClick = {
                        picked = m
                        expanded = false
                    },
                )
            }
        }
    }
    SpacerH(12)
    Button(
        onClick = {
            val pkg = picked?.pkg
            if (pkg == null) { Toast.makeText(context, "请先选择模块", Toast.LENGTH_SHORT).show(); return@Button }
            if (!client.isConfigured()) {
                Toast.makeText(context, "请先配置 AI 服务商 API", Toast.LENGTH_LONG).show()
                context.startActivity(Intent(context, AiConfigActivity::class.java))
                return@Button
            }
            result = "分析并转换中…"
            busy = true
            scope.launch {
                val conv = ModuleConverter(context)
                val desc = withContext(Dispatchers.IO) { conv.analyze(pkg) }
                result = if (desc == null) {
                    "无法分析该模块"
                } else {
                    val adapterJson = withContext(Dispatchers.IO) { conv.toAdapter(desc) }
                    Logger.i("Converted ${desc.packageName}")
                    "模块分析完成：\n入口：${desc.entryPoints}\n\n适配器描述：\n$adapterJson"
                }
                busy = false
            }
        },
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Transform, contentDescription = null)
        Text("分析并转换")
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
