package com.wangucanghai.isposed.ui.home

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.ui.ActivationActivity
import com.wangucanghai.isposed.ui.AiConfigActivity
import com.wangucanghai.isposed.ui.components.BetaBadge
import com.wangucanghai.isposed.ui.components.Dot
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.util.Logger
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Home dashboard: activation status and runtime statistics.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    var moduleTotal by remember { mutableStateOf(0) }
    var moduleEnabled by remember { mutableStateOf(0) }
    var logCount by remember { mutableStateOf(0) }
    var active by remember { mutableStateOf(prefs.adbPaired) }

    LaunchedEffect(Unit) {
        val repo = ModuleRepository(context)
        val modules = repo.loadModules()
        moduleTotal = modules.size
        moduleEnabled = modules.count { it.isEnabled }
        logCount = Logger.entries().size
        active = prefs.adbPaired
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MiuixTheme.textStyles.headline2,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                BetaBadge()
            }
        }

        item {
            SpacerH(4)
            ActivationCard(
                active = active,
                host = prefs.adbHost,
                port = prefs.adbPort,
                onClick = {
                    context.startActivity(Intent(context, ActivationActivity::class.java))
                },
            )
        }

        item { SectionTitle(stringResource(R.string.home_status_title)) }
        item {
            SectionCard {
                StatRow(stringResource(R.string.home_stat_installed), stringResource(R.string.home_count_modules, moduleTotal))
                StatRow(stringResource(R.string.home_stat_enabled), stringResource(R.string.home_count_modules, moduleEnabled))
                StatRow(stringResource(R.string.home_stat_logs), stringResource(R.string.home_count_logs, logCount))
                StatRow(
                    stringResource(R.string.home_stat_adb),
                    stringResource(if (active) R.string.home_paired else R.string.home_unpaired),
                )
                StatRow(
                    stringResource(R.string.home_stat_shell_state),
                    stringResource(if (active) R.string.home_available else R.string.home_unavailable),
                )
            }
        }

        item {
            SectionCard(onClick = { context.startActivity(Intent(context, AiConfigActivity::class.java)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    SpacerH(4)
                    Column {
                        Text(stringResource(R.string.settings_ai_config), style = MiuixTheme.textStyles.title3)
                        Text(
                            text = if (prefs.aiBaseUrl.isEmpty()) stringResource(R.string.not_configured) else prefs.aiBaseUrl,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivationCard(active: Boolean, host: String, port: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(
            color = if (active) {
                MiuixTheme.colorScheme.primaryContainer
            } else {
                MiuixTheme.colorScheme.secondaryContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(
                        color = if (active) Color(0xFF2E7D32) else MiuixTheme.colorScheme.secondary,
                        size = 10,
                    )
                    SpacerH(4)
                    Text(
                        text = stringResource(
                            if (active) R.string.activation_active else R.string.activation_inactive,
                        ),
                        style = MiuixTheme.textStyles.title2,
                        color = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Icon(
                    imageVector = if (active) Icons.Filled.CheckCircle else Icons.Filled.RocketLaunch,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            }
            SpacerH(10)
            Text(
                text = stringResource(
                    if (active) R.string.home_active_desc else R.string.home_inactive_desc,
                ),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            if (active) {
                SpacerH(8)
                Text(
                    text = "$host:$port",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            } else {
                SpacerH(8)
                Text(
                    text = stringResource(R.string.home_unlock_hint),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
        Text(value, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurface)
    }
}
