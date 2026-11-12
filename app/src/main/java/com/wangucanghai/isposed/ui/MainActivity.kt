package com.wangucanghai.isposed.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.home.HomeScreen
import com.wangucanghai.isposed.ui.logs.LogsScreen
import com.wangucanghai.isposed.ui.modules.ModulesScreen
import com.wangucanghai.isposed.ui.notice.NoticeContent
import com.wangucanghai.isposed.ui.notice.OnboardingFlow
import com.wangucanghai.isposed.ui.settings.SettingsScreen
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Single-activity Compose host with bottom navigation and mandatory agreement gate.
 *
 * Launch sequence: pending update announcement -> onboarding flow (user agreement,
 * language, open-source notice) -> per-launch user agreement reminder -> app shell.
 *
 * Copyright (C) 2026 万古沧海
 */
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                LaunchGate(onExit = { finish() })
            }
        }
    }
}

private enum class Gate { CHANGELOG, ONBOARDING, AGREEMENT, READY }

@Composable
private fun LaunchGate(onExit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }

    var gate by remember {
        mutableStateOf(
            when {
                prefs.pendingChangelogVersion.isNotEmpty() -> Gate.CHANGELOG
                prefs.agreementVersion < NoticeContent.AGREEMENT_VERSION -> Gate.ONBOARDING
                else -> Gate.AGREEMENT
            },
        )
    }

    when (gate) {
        Gate.CHANGELOG -> {
            OverlayDialog(
                show = true,
                title = "更新公告 · v${prefs.pendingChangelogVersion}",
                onDismissRequest = {},
            ) {
                Column {
                    Text(
                        text = prefs.pendingChangelogNotes.ifBlank { "本次更新包含功能优化与问题修复。" },
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        text = "我知道了",
                        onClick = {
                            prefs.pendingChangelogVersion = ""
                            prefs.pendingChangelogNotes = ""
                            gate = Gate.ONBOARDING
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Gate.ONBOARDING -> {
            OnboardingFlow(prefs = prefs, onFinished = { gate = Gate.READY })
        }

        Gate.AGREEMENT -> {
            OverlayDialog(
                show = true,
                title = NoticeContent.USER_AGREEMENT_TITLE,
                onDismissRequest = { onExit() },
            ) {
                Column {
                    Text(
                        text = NoticeContent.USER_AGREEMENT,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        text = "我已阅读并同意",
                        onClick = {
                            prefs.agreementAccepted = true
                            prefs.agreementVersion = NoticeContent.AGREEMENT_VERSION
                            gate = Gate.READY
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = "不同意并退出",
                        onClick = { onExit() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Gate.READY -> MainScaffold()
    }
}

private data class Tab(val id: String, val label: String, val icon: ImageVector)

private const val TAB_HOME = "home"
private const val TAB_MODULES = "modules"
private const val TAB_LOGS = "logs"
private const val TAB_SETTINGS = "settings"

@Composable
private fun MainScaffold() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val tabs = remember {
        listOf(
            Tab(TAB_HOME, "首页", Icons.Filled.Home),
            Tab(TAB_MODULES, "模块", Icons.Filled.Build),
            Tab(TAB_LOGS, "日志", Icons.Filled.Terminal),
            Tab(TAB_SETTINGS, "设置", Icons.Filled.Settings),
        )
    }
    // The module and log entries only unlock once the device is activated.
    val lockedTabs = setOf(TAB_MODULES, TAB_LOGS)
    var activated by remember { mutableStateOf(prefs.adbPaired) }
    var selectedId by rememberSaveable { mutableStateOf(TAB_HOME) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var promptShown by remember { mutableStateOf(false) }

    // Refresh the activation state whenever the app comes back to the
    // foreground, e.g. right after the activation screen finished.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, prefs) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activated = prefs.adbPaired
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val visibleTabs = tabs.filter { it.id !in lockedTabs || activated }
    if (visibleTabs.none { it.id == selectedId }) {
        selectedId = visibleTabs.first().id
    }

    // Auto-check for updates on every app entry, then prompt when a newer
    // release is available.
    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { UpdateChecker(context).check() }
        updateInfo = r
        if (r.hasUpdate) promptShown = true
    }

    val pendingUpdate = updateInfo?.takeIf { it.hasUpdate }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                tabs = tabs,
                lockedTabs = lockedTabs,
                unlocked = activated,
                selectedId = selectedId,
                onSelect = { selectedId = it },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = pendingUpdate != null && !promptShown,
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
                    onClick = { context.startActivity(Intent(context, UpdateActivity::class.java)) },
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
                        Column {
                            Text(
                                "发现新版本 v${pendingUpdate?.latestVersion}",
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
            Crossfade(targetState = selectedId, animationSpec = tween(260)) { tab ->
                when (tab) {
                    TAB_MODULES -> ModulesScreen(Modifier.padding(inner))
                    TAB_LOGS -> LogsScreen(Modifier.padding(inner))
                    TAB_SETTINGS -> SettingsScreen(Modifier.padding(inner))
                    else -> HomeScreen(Modifier.padding(inner).fillMaxWidth())
                }
            }
        }
    }

    if (pendingUpdate != null && promptShown) {
        OverlayDialog(
            show = true,
            title = "发现新版本 v${pendingUpdate.latestVersion}",
            summary = pendingUpdate.notes.ifBlank { "包含新功能与问题修复，建议立即更新。" },
            onDismissRequest = { promptShown = false },
        ) {
            Column {
                Button(
                    onClick = {
                        promptShown = false
                        context.startActivity(Intent(context, UpdateActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    minHeight = 46.dp,
                ) {
                    Text("立即更新")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    text = "稍后再说",
                    onClick = { promptShown = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Bottom navigation with an animated pill indicator, icon scaling and label
 * crossfade for the selected entry.
 */
@Composable
private fun BottomNavBar(
    tabs: List<Tab>,
    lockedTabs: Set<String>,
    unlocked: Boolean,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        color = MiuixTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val locked = tab.id in lockedTabs
                if (locked) {
                    // Locked entries slide in once the device gets activated.
                    AnimatedVisibility(
                        visible = unlocked,
                        enter = fadeIn(tween(320)) +
                            expandHorizontally(tween(360)) { it / 3 } +
                            scaleIn(tween(360), initialScale = 0.7f),
                        exit = fadeOut(tween(220)) +
                            shrinkHorizontally(tween(260)) { it / 3 } +
                            scaleOut(tween(260), targetScale = 0.7f),
                    ) {
                        NavEntry(
                            tab = tab,
                            selected = tab.id == selectedId,
                            onClick = { onSelect(tab.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    NavEntry(
                        tab = tab,
                        selected = tab.id == selectedId,
                        onClick = { onSelect(tab.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavEntry(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) MiuixTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(260),
        label = "navIndicatorColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "navIconScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.62f,
        animationSpec = tween(220),
        label = "navIconAlpha",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(240),
        label = "navLabelAlpha",
    )
    val iconOffset by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = tween(240),
        label = "navIconOffset",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(indicatorColor)
                .padding(horizontal = 18.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = MiuixTheme.colorScheme.onSurfaceContainer,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
                    .offset(y = iconOffset),
            )
        }
        Box(
            modifier = Modifier
                .height(16.dp)
                .alpha(labelAlpha),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = tab.label,
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }
    }
}
