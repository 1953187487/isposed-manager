package com.wangucanghai.isposed.ui.activation

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.adb.ActivationManager
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

/**
 * AxManager-style wireless debugging activation screen.
 *
 * Flow: mDNS discover -> TLS pair (6-digit code) -> TLS connect -> verify shell
 * -> optionally switch adbd to a fixed TCP port. Live log panel shows each step.
 *
 * Copyright (C) 2026 万古沧海
 */
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
    var code by remember { mutableStateOf("") }
    var tcpPort by remember { mutableStateOf("5555") }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Text("无线调试激活", style = MiuixTheme.textStyles.headline2)
        SpacerH(6)
        Text(
            text = "全程无需 Root。先开启开发者选项 → 无线调试，配对一次即可。",
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
        }

        SectionTitle("一键激活")
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    events = emptyList()
                    step = "发现设备…"
                    log("开始发现无线调试服务…")
                    val pairList = withContext(Dispatchers.IO) { manager.discoverPairingServices() }
                    if (pairList.isEmpty()) {
                        step = ""
                        log("未发现配对服务，请确保已开启「无线调试」，可点击「使用配对码配对设备」", true)
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
                    if (tcp) log("已切换 ADB 固定端口 $port，下次可直接连接")
                    step = "校验 Shell…"
                    val verify = withContext(Dispatchers.IO) { manager.verifyShell() }
                    if (verify) {
                        log("激活完成")
                        active = true
                    } else {
                        log("连接建立但 shell 校验未通过", true)
                    }
                    step = ""
                    busy = false
                }
            },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                SpacerH(4)
                Text("激活中…")
            } else {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                SpacerH(4)
                Text("开始激活")
            }
        }
        SpacerH(8)
        OutlinedButton(
            onClick = {
                scope.launch {
                    busy = true
                    log("尝试直接连接固定端口 ${prefs.adbHost}:${prefs.adbPort}…")
                    val ok = withContext(Dispatchers.IO) {
                        manager.connectTcp(prefs.adbHost, prefs.adbPort)
                    }
                    log(if (ok) "固定端口连接成功" else "固定端口连接失败（需先无线激活一次）", !ok)
                    active = ok
                    busy = false
                }
            },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
            SpacerH(4)
            Text("连接固定端口 ${prefs.adbHost}:${prefs.adbPort}")
        }

        SectionTitle("配对码")
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("6 位配对码（手机无线调试窗口显示）") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        SpacerH(8)
        OutlinedTextField(
            value = tcpPort,
            onValueChange = { tcpPort = it.filter { ch -> ch.isDigit() }.take(5) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("固定 TCP 端口（默认 5555）") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )

        SectionTitle("发现的服务")
        if (pairing.isNotEmpty()) {
            Text("配对服务", style = MiuixTheme.textStyles.footnote2)
            pairing.forEach { svc ->
                Text(
                    "${svc.instanceName}  ${svc.host}:${svc.port}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        if (connecting.isNotEmpty()) {
            SpacerH(6)
            Text("连接服务", style = MiuixTheme.textStyles.footnote2)
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
