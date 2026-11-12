package com.wangucanghai.isposed.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.LocaleHelper
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * In-app language picker, used from settings after the onboarding flow.
 *
 * Copyright (C) 2026 万古沧海
 */
data class LanguageOption(val code: String, val title: String, val desc: String)

val LANGUAGE_OPTIONS = listOf(
    LanguageOption(LocaleHelper.LANGUAGE_SYSTEM, "跟随系统", "使用设备当前语言"),
    LanguageOption(LocaleHelper.LANGUAGE_ZH, "简体中文", "中文界面"),
    LanguageOption(LocaleHelper.LANGUAGE_EN, "English", "English interface"),
)

@Composable
fun LanguagePickerDialog(
    show: Boolean,
    prefs: AppPrefs,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    OverlayDialog(show = show, title = "界面语言", onDismissRequest = onDismiss) {
        Column {
            LANGUAGE_OPTIONS.forEach { option ->
                val selected = option.code == prefs.appLanguage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            prefs.appLanguage = option.code
                            LocaleHelper.applyToSystem(context, option.code)
                            onDismiss()
                            (context as? android.app.Activity)?.recreate()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline,
                    )
                    SpacerH(10)
                    Column {
                        Text(
                            text = option.title,
                            style = MiuixTheme.textStyles.title3,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = option.desc,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }
            }
            SpacerH(4)
            SpacerH(8)
            Text(
                text = "切换后立即生效",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            SpacerH(12)
        }
    }
}
