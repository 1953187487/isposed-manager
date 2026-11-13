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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.LocaleHelper
import com.wangucanghai.isposed.ui.AboutActivity
import com.wangucanghai.isposed.ui.ActivationActivity
import com.wangucanghai.isposed.ui.AiConfigActivity
import com.wangucanghai.isposed.ui.MainActivity
import com.wangucanghai.isposed.ui.ShellActivity
import com.wangucanghai.isposed.ui.UpdateActivity
import com.wangucanghai.isposed.ui.authorization.AuthorizationActivity
import com.wangucanghai.isposed.ui.components.LanguagePickerDialog
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.SettingRow
import com.wangucanghai.isposed.ui.credits.CreditsActivity
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Settings: activation, shell, AI, authorization, language, updates and about.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    var langDialog by remember { mutableStateOf(false) }
    var noticeDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Text(stringResource(R.string.settings_title), style = MiuixTheme.textStyles.headline2)

        SectionTitle(stringResource(R.string.settings_section_activation))
        SectionCard {
            SettingRow(
                icon = Icons.Filled.RocketLaunch,
                title = stringResource(R.string.settings_activate),
                subtitle = stringResource(R.string.settings_activate_sub),
            ) { context.startActivity(Intent(context, ActivationActivity::class.java)) }
            SettingRow(
                icon = Icons.Filled.Terminal,
                title = stringResource(R.string.settings_shell),
                subtitle = stringResource(R.string.settings_shell_sub),
            ) { context.startActivity(Intent(context, ShellActivity::class.java)) }
        }

        SectionTitle(stringResource(R.string.settings_section_ai))
        SectionCard {
            SettingRow(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(R.string.settings_ai_config),
                subtitle = stringResource(R.string.settings_ai_config_sub),
            ) { context.startActivity(Intent(context, AiConfigActivity::class.java)) }
        }

        SectionTitle(stringResource(R.string.settings_section_authorization))
        SectionCard {
            SettingRow(
                icon = Icons.Filled.Security,
                title = stringResource(R.string.settings_authorization),
                subtitle = stringResource(R.string.settings_authorization_sub),
            ) { context.startActivity(Intent(context, AuthorizationActivity::class.java)) }
        }

        SectionTitle(stringResource(R.string.settings_section_general))
        SectionCard {
            SettingRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.settings_language),
                subtitle = when (prefs.appLanguage) {
                    LocaleHelper.LANGUAGE_ZH -> stringResource(R.string.lang_zh)
                    LocaleHelper.LANGUAGE_EN -> stringResource(R.string.lang_en)
                    else -> stringResource(R.string.lang_system)
                },
            ) { langDialog = true }
            SettingRow(
                icon = Icons.Filled.WavingHand,
                title = stringResource(R.string.settings_intro),
                subtitle = stringResource(R.string.settings_intro_sub),
            ) {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(MainActivity.EXTRA_SHOW_INTRO, true),
                )
            }
            SettingRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_agreement),
                subtitle = stringResource(R.string.settings_agreement_sub),
            ) { noticeDialog = R.string.notice_agreement_title to R.string.notice_agreement_body }
            SettingRow(
                icon = Icons.Filled.Group,
                title = stringResource(R.string.settings_oss_notice),
                subtitle = stringResource(R.string.settings_oss_notice_sub),
            ) { noticeDialog = R.string.notice_oss_title to R.string.notice_oss_body }
        }

        SectionTitle(stringResource(R.string.settings_section_update))
        SectionCard {
            SettingRow(
                icon = Icons.Filled.SystemUpdateAlt,
                title = stringResource(R.string.settings_update),
                subtitle = stringResource(R.string.settings_update_sub),
            ) { context.startActivity(Intent(context, UpdateActivity::class.java)) }
        }

        SectionTitle(stringResource(R.string.settings_section_about))
        SectionCard {
            SettingRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_sub),
            ) { context.startActivity(Intent(context, AboutActivity::class.java)) }
            SettingRow(
                icon = Icons.Filled.WavingHand,
                title = stringResource(R.string.settings_credits),
                subtitle = stringResource(R.string.settings_credits_sub),
            ) { context.startActivity(Intent(context, CreditsActivity::class.java)) }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 28.dp))
    }

    LanguagePickerDialog(
        show = langDialog,
        prefs = prefs,
        onDismiss = { langDialog = false },
    )

    noticeDialog?.let { (titleRes, bodyRes) ->
        OverlayDialog(
            show = true,
            title = stringResource(titleRes),
            onDismissRequest = { noticeDialog = null },
        ) {
            Column {
                Text(
                    text = stringResource(bodyRes),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
                TextButton(
                    text = stringResource(R.string.notice_ack),
                    onClick = { noticeDialog = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
