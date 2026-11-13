package com.wangucanghai.isposed.ui.activation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.adb.ActivationManager
import com.wangucanghai.isposed.adb.LanScanner
import com.wangucanghai.isposed.adb.MdnsResolver
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.components.StatusBadge
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Activation screen with three independent channels:
 *
 *  - [ActivationMode.WIRELESS]: mDNS discovery + TLS pairing (Android 11+ wireless debugging)
 *  - [ActivationMode.TCP]: direct plain-ADB TCP connection to `host:port`
 *  - [ActivationMode.COMMAND]: paste an `adb connect host:port` command and run it
 *
 * All channels authenticate with the persistent ADB identity from `AdbKeyStore`,
 * so a device that authorized the app once accepts every later channel.
 *
 * Copyright (C) 2026 万古沧海
 */
enum class ActivationMode(val label: String, val icon: ImageVector) {
    WIRELESS("无线调试", Icons.Filled.BluetoothSearching),
    TCP("TCP 激活", Icons.Filled.Lan),
    COMMAND("命令激活", Icons.Filled.Terminal),
}

/**
 * Rebuild a [TextFieldValue] after filtering its text, keeping the selection
 * inside the new bounds.
 *
 * Filtering can drop characters before the cursor, which leaves the original
 * selection pointing past the end of the filtered text. Compose rejects such a
 * value with an `IllegalArgumentException`, so the offsets are clamped here.
 */
private fun sanitizeValue(
    value: TextFieldValue,
    maxLength: Int,
    filter: (Char) -> Boolean = { true },
): TextFieldValue {
    val filtered = value.text.filter(filter).take(maxLength)
    val start = value.selection.start.coerceIn(0, filtered.length)
    val end = value.selection.end.coerceIn(0, filtered.length)
    return TextFieldValue(filtered, TextRange(start, end))
}

@Composable
fun ActivationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    val manager = remember { ActivationManager(context) }

    var events by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf("") }
    var pairing by remember { mutableStateOf<List<MdnsResolver.AdbService>>(emptyList()) }
    var connecting by remember { mutableStateOf<List<MdnsResolver.AdbService>>(emptyList()) }
    var lanDevices by remember { mutableStateOf<List<LanScanner.FoundDevice>>(emptyList()) }
    var lanScanning by remember { mutableStateOf(false) }
    var lanProgress by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ActivationMode.WIRELESS) }
    var codeValue by remember { mutableStateOf(TextFieldValue("")) }
    val code = codeValue.text
    var tcpPortValue by remember { mutableStateOf(TextFieldValue("5555", TextRange(4))) }
    val tcpPort = tcpPortValue.text
    var hostValue by remember { mutableStateOf(TextFieldValue(prefs.adbHost, TextRange(prefs.adbHost.length))) }
    val host = hostValue.text
    var commandValue by remember { mutableStateOf(TextFieldValue("")) }
    val command = commandValue.text
    var active by remember { mutableStateOf(prefs.adbPaired) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    fun log(msg: String, error: Boolean = false) {
        events = events + (msg to error)
        Logger.i(if (error) "Activation: $msg" else "Activation: $msg")
    }

    fun finishSuccess(message: String) {
        active = true
        step = ""
        busy = false
        log(message)
    }

    fun finishFailure(message: String) {
        step = ""
        busy = false
        log(message, true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Text("激活", style = MiuixTheme.textStyles.headline2)
        SpacerH(6)
        Text(
            text = "三种激活通道任选其一，全程无需 Root。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(16)

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = if (active) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
                    Canvas(Modifier.size(12.dp)) {
                        drawCircle(
                            color = dotColor,
                            alpha = if (busy) pulse else 1f,
                        )
                    }
                    SpacerH(4)
                    Text("当前状态", style = MiuixTheme.textStyles.title3)
                }
                StatusBadge(if (active) "已激活" else "未激活", active)
            }
            SpacerH(10)
            Text(
                text = if (active) "授权地址：${prefs.adbHost}:${prefs.adbPort}" else "尚未建立 ADB 授权",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            AnimatedVisibility(visible = busy, enter = fadeIn() + slideInVertically { it / 2 }) {
                SpacerH(8)
                Text(
                    text = step.ifBlank { "正在激活…" },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            if (active) {
                SpacerH(6)
                TextButton(
                    text = "断开连接",
                    onClick = {
                        manager.deactivate()
                        active = false
                        log("已断开 ADB 连接")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                )
            }
        }

        SectionTitle("激活方式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivationMode.entries.forEach { m ->
                ModeChip(
                    mode = m,
                    selected = mode == m,
                    onClick = { mode = m },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        SpacerH(12)

        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(260)))
                    .togetherWith(
                        slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(220)),
                    )
            },
            label = "activationMode",
        ) { current ->
            when (current) {
                ActivationMode.WIRELESS -> WirelessPanel(
                    busy = busy,
                    codeValue = codeValue,
                    onCodeChange = { codeValue = sanitizeValue(it, 6) { ch -> ch.isDigit() } },
                    portValue = tcpPortValue,
                    onPortChange = { tcpPortValue = sanitizeValue(it, 5) { ch -> ch.isDigit() } },
                    onStart = {
                        scope.launch {
                            try {
                                busy = true
                                events = emptyList()
                                step = "发现设备…"
                                log("开始发现无线调试服务…")
                                val pairList = withContext(Dispatchers.IO) { manager.discoverPairingServices() }
                                if (pairList.isEmpty()) {
                                    step = ""
                                    log(
                                        "未发现配对服务。请确认「开发者选项 → 无线调试」已开启，" +
                                            "并与本机处于同一局域网；也可改用 TCP 激活或命令激活。",
                                        true,
                                    )
                                    busy = false
                                    return@launch
                                }
                                pairing = pairList
                                val p = pairList.first()
                                log("发现配对服务 ${p.host}:${p.port}（${p.instanceName}）")
                                if (code.isBlank()) {
                                    step = ""
                                    log("请先输入 6 位配对码", true)
                                    busy = false
                                    return@launch
                                }
                                step = "配对中…"
                                log("正在配对…")
                                val paired = withContext(Dispatchers.IO) { manager.pair(p, code.trim()) }
                                if (!paired) {
                                    step = ""
                                    log("配对失败，请检查配对码是否正确，并确保手机已弹出配对窗口", true)
                                    busy = false
                                    return@launch
                                }
                                step = "连接中…"
                                log("配对成功，正在连接…")
                                val connList = withContext(Dispatchers.IO) { manager.discoverConnectServices() }
                                connecting = connList
                                val c = connList.firstOrNull()
                                if (c == null) {
                                    step = ""
                                    log("未发现连接服务，请返回并再次配对", true)
                                    busy = false
                                    return@launch
                                }
                                val ok = withContext(Dispatchers.IO) { manager.connect(c) }
                                if (!ok) {
                                    step = ""
                                    log("连接失败", true)
                                    busy = false
                                    return@launch
                                }
                                step = "切换固定端口…"
                                log("连接成功，shell 权限已获得")
                                val port = tcpPort.toIntOrNull() ?: 5555
                                val tcp = withContext(Dispatchers.IO) { manager.enableTcpMode(port) }
                                if (tcp) log("已切换 ADB 固定端口 $port，下次可用 TCP 激活直连")
                                step = "校验 Shell…"
                                val verify = withContext(Dispatchers.IO) { manager.verifyShell() }
                                if (verify) {
                                    finishSuccess("激活完成")
                                } else {
                                    finishFailure("连接建立但 shell 校验未通过")
                                }
                            } catch (t: Throwable) {
                                finishFailure(t.message ?: "无线调试激活异常")
                            }
                        }
                    },
                )

                ActivationMode.TCP -> TcpPanel(
                    busy = busy,
                    hostValue = hostValue,
                    onHostChange = { hostValue = it },
                    portValue = tcpPortValue,
                    onPortChange = { tcpPortValue = sanitizeValue(it, 5) { ch -> ch.isDigit() } },
                    devices = lanDevices,
                    scanning = lanScanning,
                    progress = lanProgress,
                    onScan = {
                        scope.launch {
                            try {
                                lanScanning = true
                                lanDevices = emptyList()
                                lanProgress = "准备扫描…"
                                log("开始扫描局域网 ADB 设备…")
                                var lastProgress = -1
                                val found = manager.scanLanDevices { done, total ->
                                    // Throttle: only refresh the label when the percent changes.
                                    val pct = (done * 100) / total.coerceAtLeast(1)
                                    if (pct != lastProgress) {
                                        lastProgress = pct
                                        withContext(Dispatchers.Main) {
                                            lanProgress = "扫描中 $pct% ($done/$total)"
                                        }
                                    }
                                }
                                lanScanning = false
                                lanProgress = ""
                                lanDevices = found
                                if (found.isEmpty()) {
                                    log("未发现局域网 ADB 设备，请确认设备已开启 ADB 调试并处于同一网络", true)
                                } else {
                                    log("发现 ${found.size} 个局域网 ADB 设备")
                                }
                            } catch (t: Throwable) {
                                lanScanning = false
                                lanProgress = ""
                                log("局域网扫描异常：${t.message}", true)
                            }
                        }
                    },
                    onPick = { device ->
                        hostValue = TextFieldValue(device.host, TextRange(device.host.length))
                        tcpPortValue = TextFieldValue(device.port.toString(), TextRange(device.port.toString().length))
                        log("已选择 ${device.host}:${device.port}${device.model?.let { "（$it）" } ?: ""}")
                    },
                    onStart = {
                        scope.launch {
                            try {
                                busy = true
                                events = emptyList()
                                val port = tcpPort.toIntOrNull()
                                if (port == null || port !in 1..65535) {
                                    finishFailure("端口无效，请输入 1-65535 之间的数字")
                                    return@launch
                                }
                                step = "TCP 连接中…"
                                log("正在连接 ${host.trim()}:$port …")
                                val ok = withContext(Dispatchers.IO) { manager.connectTcp(host.trim(), port) }
                                if (ok) {
                                    finishSuccess("TCP 激活成功，shell 权限已获得")
                                } else {
                                    finishFailure(
                                        "TCP 连接失败。请确认设备已开启 ADB 调试并监听该端口，" +
                                            "且本机密钥已在设备上授权（可先完成一次无线调试激活）。",
                                    )
                                }
                            } catch (t: Throwable) {
                                finishFailure(t.message ?: "TCP 连接异常")
                            }
                        }
                    },
                )

                ActivationMode.COMMAND -> CommandPanel(
                    busy = busy,
                    commandValue = commandValue,
                    onCommandChange = { commandValue = it },
                    onStart = {
                        scope.launch {
                            try {
                                busy = true
                                events = emptyList()
                                step = "解析命令…"
                                log("正在解析命令：${command.trim()}")
                                val result = withContext(Dispatchers.IO) { manager.activateByCommand(command) }
                                result.fold(
                                    onSuccess = { (h, p) ->
                                        finishSuccess("命令激活成功，已连接 $h:$p")
                                    },
                                    onFailure = {
                                        finishFailure(it.message ?: "命令激活失败")
                                    },
                                )
                            } catch (t: Throwable) {
                                finishFailure(t.message ?: "命令激活异常")
                            }
                        }
                    },
                )
            }
        }

        if (pairing.isNotEmpty() && mode == ActivationMode.WIRELESS) {
            SectionTitle("发现的服务")
            Text("配对服务", style = MiuixTheme.textStyles.footnote2)
            pairing.forEach { svc ->
                Text(
                    "${svc.instanceName}  ${svc.host}:${svc.port}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        if (connecting.isNotEmpty() && mode == ActivationMode.WIRELESS) {
            SectionTitle("连接服务")
            connecting.forEach { svc ->
                Text(
                    "${svc.instanceName}  ${svc.host}:${svc.port}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.secondary,
                )
            }
        }

        SectionTitle("激活日志")
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                if (events.isEmpty()) {
                    Text(
                        "尚无日志",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
                events.forEach { (msg, error) ->
                    Text(
                        text = msg,
                        style = MiuixTheme.textStyles.footnote1,
                        fontFamily = FontFamily.Monospace,
                        color = if (error) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 28.dp))
    }
}

@Composable
private fun ModeChip(
    mode: ActivationMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MiuixTheme.colorScheme.primaryContainer
        else MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MiuixTheme.colorScheme.onPrimaryContainer
        else MiuixTheme.colorScheme.onSurfaceContainerVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(mode.icon, contentDescription = null, modifier = Modifier.size(20.dp))
            SpacerH(4)
            Text(mode.label, style = MiuixTheme.textStyles.footnote2)
        }
    }
}

@Composable
private fun WirelessPanel(
    busy: Boolean,
    codeValue: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    portValue: TextFieldValue,
    onPortChange: (TextFieldValue) -> Unit,
    onStart: () -> Unit,
) {
    SectionCard {
        Text("无线调试激活", style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text(
            text = "适用于 Android 11 及以上。依次开启「开发者选项 → 无线调试」，" +
                "在「使用配对码配对设备」中获取 6 位配对码并填入下方。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(12)
        TextField(
            value = codeValue,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = "6 位配对码",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        TextField(
            value = portValue,
            onValueChange = onPortChange,
            modifier = Modifier.fillMaxWidth(),
            label = "固定 TCP 端口（默认 5555）",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(12)
        Button(
            onClick = onStart,
            enabled = !busy,
            cornerRadius = 14.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                SpacerH(4)
                Text("激活中…")
            } else {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text("开始无线调试激活")
            }
        }
    }
}

@Composable
private fun TcpPanel(
    busy: Boolean,
    hostValue: TextFieldValue,
    onHostChange: (TextFieldValue) -> Unit,
    portValue: TextFieldValue,
    onPortChange: (TextFieldValue) -> Unit,
    devices: List<LanScanner.FoundDevice>,
    scanning: Boolean,
    progress: String,
    onScan: () -> Unit,
    onPick: (LanScanner.FoundDevice) -> Unit,
    onStart: () -> Unit,
) {
    SectionCard {
        Text("TCP 激活", style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text(
            text = "设备已通过「adb tcpip 端口」开启明文 ADB 调试时使用。可先自动扫描局域网内可连接的设备，" +
                "也可直接填入设备 IP 与端口。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(12)
        Surface(
            onClick = onScan,
            enabled = !scanning && !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MiuixTheme.colorScheme.primaryContainer,
            contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Lan, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                SpacerH(6)
                Text(
                    text = if (scanning) progress.ifBlank { "正在扫描…" } else "扫描局域网设备",
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
        if (devices.isNotEmpty()) {
            SpacerH(10)
            Text(
                text = "发现 ${devices.size} 个设备，点击即可填入",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
            )
            SpacerH(6)
            devices.forEach { device ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPick(device) },
                    shape = RoundedCornerShape(12.dp),
                    color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${device.host}:${device.port}",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        if (device.model != null) {
                            Text(
                                text = device.model,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                        }
                    }
                }
                SpacerH(6)
            }
        }
        SpacerH(12)
        TextField(
            value = hostValue,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            label = "设备 IP / 主机名",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        TextField(
            value = portValue,
            onValueChange = onPortChange,
            modifier = Modifier.fillMaxWidth(),
            label = "端口（默认 5555）",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(12)
        Button(
            onClick = onStart,
            enabled = !busy,
            cornerRadius = 14.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                SpacerH(4)
                Text("连接中…")
            } else {
                Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text("连接 TCP 设备")
            }
        }
    }
}

@Composable
private fun CommandPanel(
    busy: Boolean,
    commandValue: TextFieldValue,
    onCommandChange: (TextFieldValue) -> Unit,
    onStart: () -> Unit,
) {
    SectionCard {
        Text("命令激活", style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text(
            text = "直接输入 ADB 连接命令，例如 adb connect 192.168.1.2:5555，" +
                "也可只填 192.168.1.2:5555。应用会解析地址并完成 TCP 激活。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(12)
        TextField(
            value = commandValue,
            onValueChange = onCommandChange,
            modifier = Modifier.fillMaxWidth(),
            label = "adb connect 主机:端口",
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(12)
        Button(
            onClick = onStart,
            enabled = !busy,
            cornerRadius = 14.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                SpacerH(4)
                Text("执行中…")
            } else {
                Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text("执行激活命令")
            }
        }
    }
}
