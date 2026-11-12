package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.home.HomeScreen
import com.wangucanghai.isposed.ui.logs.LogsScreen
import com.wangucanghai.isposed.ui.modules.ModulesScreen
import com.wangucanghai.isposed.ui.notice.AgreementGate
import com.wangucanghai.isposed.ui.notice.NoticeContent
import com.wangucanghai.isposed.ui.settings.SettingsScreen
import com.wangucanghai.isposed.ui.theme.IsposedTheme

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
    val tabs = remember {
        listOf(
            Tab("首页", Icons.Filled.Home),
            Tab("模块", Icons.Filled.Build),
            Tab("日志", Icons.Filled.Terminal),
            Tab("设置", Icons.Filled.Settings),
        )
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { inner ->
        when (selected) {
            0 -> HomeScreen(Modifier.padding(inner))
            1 -> ModulesScreen(Modifier.padding(inner))
            2 -> LogsScreen(Modifier.padding(inner))
            else -> SettingsScreen(Modifier.padding(inner))
        }
    }
}
