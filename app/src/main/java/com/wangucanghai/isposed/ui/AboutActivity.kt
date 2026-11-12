package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Code
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * About page: LSPosed foundation, rewritten module features, author and license.
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

private val LSPOSED_BRAND = Color(0xFFF48FB1)
private val LSPOSED_REPO = "https://github.com/LSPosed/LSPosed"

private data class RewrittenFeature(val icon: ImageVector, val title: String, val desc: String)

private val REWRITTEN_FEATURES = listOf(
    RewrittenFeature(Icons.Filled.Build, "模块检测与管理", "基于 LSPosed 守护进程接口，重新实现模块发现、启停与状态管理"),
    RewrittenFeature(Icons.Filled.CheckCircle, "作用域配置", "逐应用作用域选择与持久化，重写为本地数据驱动"),
    RewrittenFeature(Icons.Filled.Wifi, "无线调试激活", "免 Root 通过 ADB 无线配对激活，全程无需 Magisk"),
    RewrittenFeature(Icons.Filled.Terminal, "Shell 终端", "内置终端，可对已激活设备直接执行命令"),
    RewrittenFeature(Icons.Filled.AutoAwesome, "AI 转换", "一键将现有 Xposed 模块源码转换为可安装工程"),
    RewrittenFeature(Icons.Filled.Code, "AI 开发", "由 AI 直接生成完整的 LSPosed 模块工程"),
    RewrittenFeature(Icons.Filled.Update, "应用内更新", "检查、下载并安装最新正式版"),
)

@Composable
private fun AboutContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            PulsingLogo()
            Spacer(Modifier.height(12.dp))
            Text(
                "Isposed Manager",
                style = MiuixTheme.textStyles.headline2,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "v${versionName(context)}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "以 LSPosed 框架为底层，模块功能全部重写",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
        }

        SectionTitle("底层框架")
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LSPOSED_BRAND),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.lsposed_ic_launcher_foreground),
                        contentDescription = "LSPosed",
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("LSPosed Framework", style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
                    Text(
                        "GPL-3.0 · Android 8.1 ~ 14",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
            SpacerH(10)
            Text(
                "本软件基于 LSPosed Framework 深度开发，以 LSPosed 作为模块运行与注入的底层框架。LSPosed 是 Riru / Zygisk 模块，基于 LSPlant 提供与原生 Xposed 一致的 ART Hook 接口。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            SpacerH(8)
            Text(
                "模块检测、作用域、守护进程接口均来自 LSPosed 的稳定接口，本软件在此基础上重新实现了全部管理能力，严格遵守上游 GPL-3.0 二改协议并保留版权声明。",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }

        SectionTitle("模块功能彻底重写")
        SectionCard {
            REWRITTEN_FEATURES.forEach { f ->
                FeatureRow(f)
            }
        }

        SectionTitle("作者")
        SectionCard {
            Text("万古沧海", style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
            SpacerH(6)
            Text(
                "B站 · 同名账号",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "GitHub · 1953187487",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }

        SectionTitle("开源声明")
        SectionCard {
            val expanded = remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = !expanded.value },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "查看开源许可与版权声明",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    modifier = Modifier.rotate(if (expanded.value) 180f else 0f),
                )
            }
            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    SpacerH(8)
                    Text(
                        LICENSE_TEXT,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
        }
        SpacerH(28)
    }
}

@Composable
private fun PulsingLogo() {
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(LSPOSED_BRAND),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.lsposed_ic_launcher_foreground),
            contentDescription = "LSPosed",
            modifier = Modifier.size(72.dp * scale),
        )
    }
}

@Composable
private fun FeatureRow(f: RewrittenFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                f.icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(f.title, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Medium)
            Text(
                f.desc,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

private val LICENSE_TEXT = """
    本软件整体以 GPL-3.0 协议开源。

    底层框架 LSPosed (GPL-3.0)
    LSPosed Framework 由 LSPosed Contributors 维护。
    本软件以 LSPosed 为底层进行开发，模块管理功能已彻底重写。
    保留上游版权声明与 GPL-3.0 许可文本。

    原作者/上游项目：
    LSPosed (GPL-3.0) - 底层 Hook 框架
    AxManager / Axeron (Apache-2.0) - 无线调试激活/shell/插件安装参考

    遵守上述项目的开源二改协议，保留上游版权声明与许可文本。
""".trimIndent()

private fun versionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.3"
} catch (_: Exception) { "1.0.3" }
