package com.wangucanghai.isposed.ui

import top.yukonga.miuix.kmp.theme.MiuixTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.home.HomeScreen
import com.wangucanghai.isposed.ui.logs.LogsScreen
import com.wangucanghai.isposed.ui.modules.ModulesScreen
import com.wangucanghai.isposed.ui.notice.AgreementGate
import com.wangucanghai.isposed.ui.notice.NoticeContent
import com.wangucanghai.isposed.ui.settings.SettingsScreen
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text

/**
 * Single-activity Compose host with bottom navigation and mandatory agreement gate.
 *
 * Copyright (C) 2026 万古沧海
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = AppPrefs(this)
        setContent {
            IsposedTheme {
                val needsAgreement = prefs.agreementVersion < NoticeContent.AGREEMENT_VERSION
                var agreed by remember { mutableStateOf(!needsAgreement) }
                if (!agreed) {
                    AgreementGate(prefs = prefs, onAccepted = { agreed = true })
                } else {
                    MainScaffold()
                }
            }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
private fun MainScaffold() {
    val context = LocalContext.current
    val tabs = remember {
        listOf(
            Tab("首页", Icons.Filled.Home),
            Tab("模块", Icons.Filled.Build),
            Tab("日志", Icons.Filled.Terminal),
            Tab("设置", Icons.Filled.Settings),
        )
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    // Auto-check for updates on every app entry. Silently records the result and
    // shows a banner when a newer release exists.
    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { UpdateChecker(context).check() }
        updateInfo = r
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = tab.icon,
                        label = tab.label,
                    )
                }
            }
        },
    ) { inner ->
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
            val up = updateInfo
            AnimatedVisibility(
                visible = up != null && up.hasUpdate,
                enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 3 },
                exit = fadeOut(tween(220)) + shrinkVertically(tween(220)),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    cornerRadius = 16.dp,
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.tertiaryContainer,
                    ),
                    onClick = { context.startActivity(android.content.Intent(context, UpdateActivity::class.java)) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdateAlt,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        androidx.compose.foundation.layout.Column {
                            Text(
                                "发现新版本 v${up?.latestVersion}",
                                style = MiuixTheme.textStyles.title3,
                                color = MiuixTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "点击查看并更新",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
            Crossfade(targetState = selected, animationSpec = tween(260)) { tab ->
                when (tab) {
                    0 -> HomeScreen(Modifier.padding(inner).fillMaxWidth(), onModules = { selected = 1 })
                    1 -> ModulesScreen(Modifier.padding(inner))
                    2 -> LogsScreen(Modifier.padding(inner))
                    else -> SettingsScreen(Modifier.padding(inner))
                }
            }
        }
    }
}
