package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme

/**
 * About page: developer author (万古沧海 / B站同名), upstream projects and licenses.
 *
 * Copyright (C) 2026 万古沧海
 */
class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "关于本应用", onBack = { finish() }) {
                    AboutContent()
                }
            }
        }
    }
}

@Composable
private fun AboutContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Text(
        "Isposed Manager",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.verticalScroll(rememberScrollState()),
    )
    SpacerH(4)
    Text(
        "v${versionName(context)} · 测试版",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SpacerH(16)
    SectionCard {
        Text("作者", style = MaterialTheme.typography.titleSmall)
        SpacerH(8)
        Text("万古沧海", style = MaterialTheme.typography.bodyLarge)
        Text("B站 · 同名账号", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("GitHub · 1953187487", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SpacerH(16)
    SectionCard {
        Text("第三方开源声明", style = MaterialTheme.typography.titleSmall)
        SpacerH(8)
        Text(
            THIRD_PARTY,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
    SpacerH(28)
}

private val THIRD_PARTY = """
    LSPosed (GPL-3.0)
    https://github.com/LSPosed/LSPosed
    引入：模块检测/作用域/守护进程接口

    AxManager / Axeron (Apache-2.0)
    https://github.com/fahrez182/AxManager
    引入：无线调试激活/shell/插件安装/BusyBox

    本软件整体以 GPL-3.0 开源。
    严格遵守上述两个项目的开源二改协议，
    保留上游版权声明与许可文本。
""".trimIndent()

private fun versionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.1"
} catch (_: Exception) { "1.0.1" }
