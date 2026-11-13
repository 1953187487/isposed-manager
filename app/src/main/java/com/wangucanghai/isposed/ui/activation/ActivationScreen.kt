package com.wangucanghai.isposed.ui.activation

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.adb.ActivationManager
import com.wangucanghai.isposed.adb.DiscoveredDevice
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
enum class ActivationMode(val labelRes: Int, val icon: ImageVector) {
    WIRELESS(R.string.activation_mode_wireless, Icons.Filled.BluetoothSearching),
    TCP(R.string.activation_mode_tcp, Icons.Filled.Lan),
    COMMAND(R.string.activation_mode_command, Icons.Filled.Terminal),
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
    var devices by remember { mutableStateOf<List<DiscoveredDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
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

    fun logRes(res: Int, args: List<Any> = emptyList(), error: Boolean = false) {
        val msg = context.getString(res, *args.toTypedArray())
        log(msg, error)
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
        Text(stringResource(R.string.activation_title), style = MiuixTheme.textStyles.headline2)
        SpacerH(6)
        Text(
            text = stringResource(R.string.activation_desc),
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
                    Text(stringResource(R.string.activation_status), style = MiuixTheme.textStyles.title3)
                }
                StatusBadge(
                    if (active) stringResource(R.string.activation_active)
                    else stringResource(R.string.activation_inactive),
                    active,
                )
            }
            SpacerH(10)
            Text(
                text = if (active) {
                    stringResource(R.string.activation_auth_addr, prefs.adbHost, prefs.adbPort)
                } else {
                    stringResource(R.string.activation_no_auth)
                },
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            AnimatedVisibility(visible = busy, enter = fadeIn() + slideInVertically { it / 2 }) {
                SpacerH(8)
                Text(
                    text = step.ifBlank { stringResource(R.string.activation_in_progress) },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            if (active) {
                SpacerH(6)
                TextButton(
                    text = stringResource(R.string.activation_disconnect),
                    onClick = {
                        manager.deactivate()
                        active = false
                        log(context.getString(R.string.activation_disconnected))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                )
            }
        }

        SectionTitle(stringResource(R.string.activation_methods))
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
                                step = context.getString(R.string.activation_step_discover)
                                log(context.getString(R.string.activation_step_discovering))
                                val pairList = withContext(Dispatchers.IO) { manager.discoverPairingServices() }
                                if (pairList.isEmpty()) {
                                    step = ""
                                    log(context.getString(R.string.activation_step_no_pairing), true)
                                    busy = false
                                    return@launch
                                }
                                pairing = pairList
                                val p = pairList.first()
                                log(context.getString(R.string.activation_step_pairing_found, p.host, p.port, p.instanceName))
                                if (code.isBlank()) {
                                    step = ""
                                    log(context.getString(R.string.activation_step_need_code), true)
                                    busy = false
                                    return@launch
                                }
                                step = context.getString(R.string.activation_step_pairing)
                                log(context.getString(R.string.activation_step_pairing_doing))
                                val paired = withContext(Dispatchers.IO) { manager.pair(p, code.trim()) }
                                if (!paired) {
                                    step = ""
                                    log(context.getString(R.string.activation_step_pairing_failed), true)
                                    busy = false
                                    return@launch
                                }
                                step = context.getString(R.string.activation_step_connecting)
                                log(context.getString(R.string.activation_step_connecting_doing))
                                val connList = withContext(Dispatchers.IO) { manager.discoverConnectServices() }
                                connecting = connList
                                val c = connList.firstOrNull()
                                if (c == null) {
                                    step = ""
                                    log(context.getString(R.string.activation_step_no_connect), true)
                                    busy = false
                                    return@launch
                                }
                                val ok = withContext(Dispatchers.IO) { manager.connect(c) }
                                if (!ok) {
                                    step = ""
                                    log(context.getString(R.string.activation_step_connect_failed), true)
                                    busy = false
                                    return@launch
                                }
                                step = context.getString(R.string.activation_step_tcp_mode)
                                log(context.getString(R.string.activation_step_shell_ok))
                                val port = tcpPort.toIntOrNull() ?: 5555
                                val tcp = withContext(Dispatchers.IO) { manager.enableTcpMode(port) }
                                if (tcp) {
                                    log(context.getString(R.string.activation_step_port_switched, port))
                                }
                                step = context.getString(R.string.activation_step_verify)
                                val verify = withContext(Dispatchers.IO) { manager.verifyShell() }
                                if (verify) {
                                    finishSuccess(context.getString(R.string.activation_step_done))
                                } else {
                                    finishFailure(context.getString(R.string.activation_step_shell_failed))
                                }
                            } catch (t: Throwable) {
                                finishFailure(t.message ?: context.getString(R.string.activation_step_wireless_exception))
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
                    devices = devices,
                    scanning = scanning,
                    progress = progress,
                    onScan = {
                        scope.launch {
                            try {
                                scanning = true
                                devices = emptyList()
                                progress = context.getString(R.string.activation_step_scan_prepare)
                                events = emptyList()
                                var lastProgress = -1
                                val found = manager.discoverDevices(
                                    onLog = { event ->
                                        logRes(event.messageRes, event.args, event.isError)
                                    },
                                    onProgress = { done, total ->
                                        val pct = (done * 100) / total.coerceAtLeast(1)
                                        if (pct != lastProgress) {
                                            lastProgress = pct
                                            withContext(Dispatchers.Main) {
                                                progress = context.getString(R.string.activation_step_scan_progress, pct, done, total)
                                            }
                                        }
                                    },
                                )
                                scanning = false
                                progress = ""
                                devices = found
                                if (found.isEmpty()) {
                                    log(context.getString(R.string.activation_step_scan_empty), true)
                                } else {
                                    log(context.getString(R.string.activation_step_scan_found, found.size))
                                }
                            } catch (t: Throwable) {
                                scanning = false
                                progress = ""
                                log(context.getString(R.string.activation_step_scan_exception, t.message ?: ""), true)
                            }
                        }
                    },
                    onPick = { device ->
                        hostValue = TextFieldValue(device.host, TextRange(device.host.length))
                        tcpPortValue = TextFieldValue(device.port.toString(), TextRange(device.port.toString().length))
                        log(
                            context.getString(
                                R.string.activation_step_device_selected,
                                device.host,
                                device.port,
                                device.model?.let { "（$it）" } ?: "",
                            ),
                        )
                    },
                    onStart = {
                        scope.launch {
                            try {
                                busy = true
                                events = emptyList()
                                val port = tcpPort.toIntOrNull()
                                if (port == null || port !in 1..65535) {
                                    finishFailure(context.getString(R.string.activation_step_port_invalid))
                                    return@launch
                                }
                                step = context.getString(R.string.activation_step_tcp_connecting)
                                log(context.getString(R.string.activation_step_tcp_doing, host.trim(), port))
                                val ok = withContext(Dispatchers.IO) { manager.connectTcp(host.trim(), port) }
                                if (ok) {
                                    finishSuccess(context.getString(R.string.activation_step_tcp_ok))
                                } else {
                                    finishFailure(context.getString(R.string.activation_step_tcp_failed))
                                }
                            } catch (t: Throwable) {
                                finishFailure(t.message ?: context.getString(R.string.activation_step_tcp_exception))
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
                                step = context.getString(R.string.activation_step_cmd_parse)
                                log(context.getString(R.string.activation_step_cmd_doing, command.trim()))
                                val result = withContext(Dispatchers.IO) { manager.activateByCommand(command) }
                                result.fold(
                                    onSuccess = { (h, p) ->
                                        finishSuccess(context.getString(R.string.activation_step_cmd_ok, h, p))
                                    },
                                    onFailure = {
                                        finishFailure(it.message ?: context.getString(R.string.activation_step_cmd_failed))
                                    },
                                )
                            } catch (t: Throwable) {
                                finishFailure(t.message ?: context.getString(R.string.activation_step_cmd_exception))
                            }
                        }
                    },
                )
            }
        }

        if (pairing.isNotEmpty() && mode == ActivationMode.WIRELESS) {
            SectionTitle(stringResource(R.string.activation_services))
            Text(stringResource(R.string.activation_pairing_services), style = MiuixTheme.textStyles.footnote2)
            pairing.forEach { svc ->
                Text(
                    "${svc.instanceName}  ${svc.host}:${svc.port}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        if (connecting.isNotEmpty() && mode == ActivationMode.WIRELESS) {
            SectionTitle(stringResource(R.string.activation_connect_services))
            connecting.forEach { svc ->
                Text(
                    "${svc.instanceName}  ${svc.host}:${svc.port}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.secondary,
                )
            }
        }

        SectionTitle(stringResource(R.string.activation_log_title))
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
                        stringResource(R.string.activation_log_empty),
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
        if (events.isNotEmpty()) {
            TextButton(
                text = stringResource(R.string.copy_log),
                onClick = {
                    val text = events.joinToString("\n") { it.first }
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("activation_log", text))
                },
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
            )
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
            Text(stringResource(mode.labelRes), style = MiuixTheme.textStyles.footnote2)
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
        Text(stringResource(R.string.activation_wireless_title), style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text(
            text = stringResource(R.string.activation_wireless_desc),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(12)
        TextField(
            value = codeValue,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.activation_wireless_code),
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        TextField(
            value = portValue,
            onValueChange = onPortChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.activation_wireless_port),
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
                Text(stringResource(R.string.activation_wireless_running))
            } else {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text(stringResource(R.string.activation_wireless_start))
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
    devices: List<DiscoveredDevice>,
    scanning: Boolean,
    progress: String,
    onScan: () -> Unit,
    onPick: (DiscoveredDevice) -> Unit,
    onStart: () -> Unit,
) {
    SectionCard {
        Text(stringResource(R.string.activation_tcp_title), style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text(
            text = stringResource(R.string.activation_tcp_desc),
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
                    text = if (scanning) progress.ifBlank { stringResource(R.string.activation_tcp_scanning) }
                    else stringResource(R.string.activation_tcp_scan),
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
        if (devices.isNotEmpty()) {
            SpacerH(10)
            Text(
                text = stringResource(R.string.activation_tcp_found, devices.size),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
            )
            SpacerH(6)
            devices.forEach { device ->
                DeviceRow(device = device, onPick = { onPick(device) })
                SpacerH(6)
            }
        }
        SpacerH(12)
        TextField(
            value = hostValue,
            onValueChange = onHostChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.activation_tcp_host),
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        TextField(
            value = portValue,
            onValueChange = onPortChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.activation_tcp_port),
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
                Text(stringResource(R.string.activation_tcp_connecting))
            } else {
                Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text(stringResource(R.string.activation_tcp_connect))
            }
        }
    }
}

@Composable
private fun DeviceRow(device: DiscoveredDevice, onPick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPick,
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
                text = device.endpoint,
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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MiuixTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = when {
                        device.tls -> stringResource(R.string.activation_tcp_badge_tls)
                        device.authorized -> stringResource(R.string.activation_tcp_badge_authorized)
                        else -> stringResource(R.string.activation_tcp_badge_pending)
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
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
        Text(stringResource(R.string.activation_command_title), style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text(
            text = stringResource(R.string.activation_command_desc),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(12)
        TextField(
            value = commandValue,
            onValueChange = onCommandChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.activation_command_field),
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
                Text(stringResource(R.string.activation_command_running))
            } else {
                Icon(Icons.Filled.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text(stringResource(R.string.activation_command_run))
            }
        }
    }
}
