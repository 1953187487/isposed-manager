package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.adb.ShellExecutor
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SectionCard
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
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Root-free shell terminal (WebUI-style quick shell).
 *
 * Copyright (C) 2026 万古沧海
 */
class ShellActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "Shell 终端", onBack = { finish() }) {
                    ShellContent()
                }
            }
        }
    }
}

@Composable
private fun ShellContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cmd by remember { mutableStateOf(TextFieldValue("")) }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    TextField(
        value = cmd,
        onValueChange = { cmd = it },
        modifier = Modifier.fillMaxWidth(),
        label = "输入命令，如 getprop ro.product.model",
        useLabelAsPlaceholder = true,
        singleLine = true,
        cornerRadius = 14.dp,
    )
    SpacerH(10)
    Button(
        onClick = {
            val c = cmd.text.trim()
            if (c.isEmpty()) return@Button
            running = true
            output = "> $c\n"
            scope.launch {
                val shell = ShellExecutor(AppPrefs(context))
                val r = withContext(Dispatchers.IO) { shell.exec(c) }
                output = "> $c\n\n${r.output}"
                running = false
                Logger.i("shell: $c -> ${r.output.take(100)}")
            }
        },
        enabled = !running,
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Text("运行")
    }
    SpacerH(16)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().animateContentSize().heightIn(min = 200.dp, max = 480.dp),
    ) {
        Text(
            text = output.ifBlank { "尚无输出" },
            style = MiuixTheme.textStyles.footnote1,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        )
    }
    SpacerH(28)
}
