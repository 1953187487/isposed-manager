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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Transform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(14.dp),
        color = MiuixTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                picked?.label ?: "未选择模块",
                style = MiuixTheme.textStyles.body1,
                color = if (picked != null) {
                    MiuixTheme.colorScheme.onSecondaryContainer
                } else {
                    MiuixTheme.colorScheme.onSurfaceContainerVariant
                },
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
    OverlayDialog(
        show = expanded,
        title = "选择模块",
        onDismissRequest = { expanded = false },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            if (modules.isEmpty()) {
                Text(
                    "未发现可转换的模块",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            } else {
                modules.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                picked = m
                                expanded = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            m.label,
                            style = MiuixTheme.textStyles.body2,
                            color = if (picked?.pkg == m.pkg) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                        )
                        if (picked?.pkg == m.pkg) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                            )
                        }
                    }
                }
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
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Transform, contentDescription = null)
        Text("分析并转换")
    }
    SpacerH(16)
    if (result.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().animateContentSize(),
        ) {
            Text(
                text = result,
                style = MiuixTheme.textStyles.footnote1,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
    }
    SpacerH(28)
}
