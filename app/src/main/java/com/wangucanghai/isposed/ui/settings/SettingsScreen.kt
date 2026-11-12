package com.wangucanghai.isposed.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.LocaleHelper
import com.wangucanghai.isposed.ui.AboutActivity
import com.wangucanghai.isposed.ui.ActivationActivity
import com.wangucanghai.isposed.ui.AiConfigActivity
import com.wangucanghai.isposed.ui.ShellActivity
import com.wangucanghai.isposed.ui.UpdateActivity
import com.wangucanghai.isposed.ui.components.LanguagePickerDialog
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.SettingRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Settings: activation, shell, AI, updates and about.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    var langDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Text("设置", style = MiuixTheme.textStyles.headline2)

        SectionTitle("激活与调试")
        SectionCard {
            SettingRow(
                icon = Icons.Filled.RocketLaunch,
                title = "激活",
                subtitle = "无线调试 / TCP / 命令 三种方式，免 Root",
            ) { context.startActivity(Intent(context, ActivationActivity::class.java)) }
            SettingRow(
                icon = Icons.Filled.Terminal,
                title = "Shell 终端",
                subtitle = "在设备上直接运行命令",
            ) { context.startActivity(Intent(context, ShellActivity::class.java)) }
        }

        SectionTitle("AI")
        SectionCard {
            SettingRow(
                icon = Icons.Filled.AutoAwesome,
                title = "AI 服务商配置",
                subtitle = "Base URL / API Key / 模型 / 温度",
            ) { context.startActivity(Intent(context, AiConfigActivity::class.java)) }
        }

        SectionTitle("通用")
        SectionCard {
            SettingRow(
                icon = Icons.Filled.Language,
                title = "界面语言",
                subtitle = when (prefs.appLanguage) {
                    LocaleHelper.LANGUAGE_ZH -> "简体中文"
                    LocaleHelper.LANGUAGE_EN -> "English"
                    else -> "跟随系统"
                },
            ) { langDialog = true }
        }

        SectionTitle("更新")
        SectionCard {
            SettingRow(
                icon = Icons.Filled.SystemUpdateAlt,
                title = "检查更新",
                subtitle = "对比 GitHub 最新版本",
            ) { context.startActivity(Intent(context, UpdateActivity::class.java)) }
        }

        SectionTitle("关于")
        SectionCard {
            SettingRow(
                icon = Icons.Filled.Info,
                title = "关于本应用",
                subtitle = "版本、作者、开源许可",
            ) { context.startActivity(Intent(context, AboutActivity::class.java)) }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 28.dp))
    }

    LanguagePickerDialog(
        show = langDialog,
        prefs = prefs,
        onDismiss = { langDialog = false },
    )
}
