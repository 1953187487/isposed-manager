package com.wangucanghai.isposed.adb

import android.content.Context
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One step of the discovery log.
 *
 * The message is kept as a resource id so the UI can render it in the active
 * language instead of a hard coded string.
 */
data class DiscoveryEvent(
    val messageRes: Int,
    val args: List<Any> = emptyList(),
    val isError: Boolean = false,
)

/**
 * AxManager-style activation orchestration.
 *
 * Flow: mDNS discover the pairing/connect services -> TLS pair with the 6-digit
 * code -> TLS connect -> verify shell -> optionally switch adbd to a fixed TCP
 * port so future activations need no wireless pairing.
 *
 * Derived from AxManager's wireless-debugging activation (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class ActivationManager(private val context: Context) {

    enum class Stage { IDLE, DISCOVERING, PAIRING, CONNECTING, TCP_MODE, READY, FAILED }

    data class Event(val stage: Stage, val message: String, val isError: Boolean = false)

    private val prefs = AppPrefs(context)
    private val resolver = MdnsResolver()
    private val keys = AdbKeyStore(context)

    @Volatile
    private var tlsClient: AdbTlsClient? = null

    @Volatile
    private var tcpClient: AdbClient? = null

    @Volatile
    private var active = false

    private val lock = Any()

    fun discoverPairingServices(timeoutMs: Int = 4000): List<MdnsResolver.AdbService> {
        Logger.i("mDNS discover pairing services")
        return resolver.discover(MdnsResolver.TYPE_PAIRING, timeoutMs)
    }

    fun discoverConnectServices(timeoutMs: Int = 4000): List<MdnsResolver.AdbService> {
        Logger.i("mDNS discover connect services")
        return resolver.discover(MdnsResolver.TYPE_CONNECT, timeoutMs)
    }

    /** Pair with the device using the pairing endpoint + 6-digit code. */
    fun pair(service: MdnsResolver.AdbService, code: String): Boolean {
        val c = AdbTlsClient(service.host, service.port, keys)
        return try {
            c.connect(pairing = true)
            val ok = c.isAuthenticated
            Logger.i("pairing with ${service.host}:${service.port} -> $ok")
            c.close()
            if (ok) {
                prefs.adbHost = service.host
                prefs.adbPaired = true
            }
            ok
        } catch (t: Throwable) {
            Logger.e("pair failed: ${t.message}")
            c.close()
            false
        }
    }

    /**
     * Scan the local network for reachable ADB devices, merging the plain
     * subnet sweep with the wireless debugging services announced over mDNS.
     *
     * Every failure is contained here: the caller receives an empty list and a
     * log line instead of an exception that would tear down the UI scope.
     *
     * @param onLog called with each human readable step of the discovery
     * @param onProgress receives (done, total) probe counts
     */
    suspend fun discoverDevices(
        onLog: suspend (DiscoveryEvent) -> Unit = {},
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        try {
            val networks = NetworkDiscovery.activeLocalNetworks(context)
            if (networks.isEmpty()) {
                onLog(DiscoveryEvent(R.string.activation_tcp_log_no_network, isError = true))
                return@withContext emptyList()
            }
            onLog(
                DiscoveryEvent(
                    R.string.activation_tcp_log_networks,
                    listOf(NetworkDiscovery.describe(networks)),
                ),
            )

            val hosts = NetworkDiscovery.candidateHosts(networks)
            onLog(
                DiscoveryEvent(
                    R.string.activation_tcp_log_candidates,
                    listOf(networks.size, hosts.size),
                ),
            )

            val plain = LanScanner(keys.key()).scan(networks, onProgress)
            onLog(
                DiscoveryEvent(
                    R.string.activation_tcp_log_plain_done,
                    listOf(plain.size),
                ),
            )

            val tls = runCatching { resolver.discover(MdnsResolver.TYPE_CONNECT, 4000) }
                .getOrElse { emptyList() }
            onLog(
                if (tls.isEmpty()) {
                    DiscoveryEvent(R.string.activation_tcp_log_mdns_none)
                } else {
                    DiscoveryEvent(R.string.activation_tcp_log_mdns_found, listOf(tls.size))
                },
            )

            buildList {
                plain.forEach { add(DiscoveredDevice(it.host, it.port, it.model, tls = false, authorized = it.authorized)) }
                tls.forEach { s ->
                    add(DiscoveredDevice(s.host, s.port, null, tls = true, authorized = true, service = s))
                }
            }.distinctBy { "${it.host}:${it.port}" }.sortedBy { it.host }
        } catch (t: Throwable) {
            Logger.e("discoverDevices failed: ${t.message}")
            onLog(
                DiscoveryEvent(
                    R.string.activation_tcp_log_exception,
                    listOf(t.message ?: "unknown"),
                    isError = true,
                ),
            )
            emptyList()
        }
    }

    /** Connect to a discovered device, choosing the TLS or plain channel. */
    fun connectDevice(device: DiscoveredDevice): Boolean =
        if (device.tls && device.service != null) connect(device.service)
        else connectTcp(device.host, device.port)

    /** Connect to a known TLS connect service directly (no pairing). */
    fun connect(service: MdnsResolver.AdbService): Boolean {
        closeClient()
        val c = AdbTlsClient(service.host, service.port, keys)
        return try {
            c.connect(pairing = false)
            if (!c.isAuthenticated) {
                Logger.e("connect not authenticated - pair first")
                c.close()
                return false
            }
            val out = c.shell("echo ISPOSED_OK && id")
            Logger.i("connect verified: $out")
            if (out.contains("ISPOSED_OK")) {
                synchronized(lock) {
                    tlsClient = c
                    active = true
                }
                prefs.adbHost = service.host
                prefs.adbPort = service.port
                prefs.adbPaired = true
                true
            } else {
                Logger.e("connect shell check failed: $out")
                c.close()
                false
            }
        } catch (t: Throwable) {
            Logger.e("connect failed: ${t.message}")
            c.close()
            false
        }
    }

    /**
     * Connect directly to a known plain-ADB TCP port (e.g. fixed 5555 after
     * `adb tcpip`). Uses the persistent ADB key so a previously paired device
     * authorizes us without needing wireless pairing again.
     */
    fun connectTcp(host: String, port: Int): Boolean {
        closeClient()
        val h = host.trim()
        val p = port
        if (h.isEmpty() || p !in 1..65535) {
            Logger.e("connectTcp invalid target $h:$p")
            return false
        }
        return try {
            val c = AdbClient(h, p, keys.key())
            c.connect()
            if (!c.isAuthenticated) {
                c.close()
                Logger.e("connectTcp not authenticated (key not yet authorized on device)")
                return false
            }
            val out = c.shell("echo ISPOSED_OK && id")
            if (out.contains("ISPOSED_OK")) {
                synchronized(lock) {
                    tcpClient = c
                    active = true
                }
                prefs.adbHost = h
                prefs.adbPort = p
                prefs.adbPaired = true
                Logger.i("connectTcp verified $h:$p")
                true
            } else {
                c.close()
                false
            }
        } catch (t: Throwable) {
            Logger.e("connectTcp failed: ${t.message}")
            false
        }
    }

    /**
     * Activate from a user supplied command such as `adb connect 192.168.1.2:5555`
     * or a bare `192.168.1.2:5555` endpoint.
     */
    fun activateByCommand(command: String): Result<Pair<String, Int>> {
        val target = parseAdbTarget(command)
            ?: return Result.failure(
                IllegalArgumentException(context.getString(R.string.activation_cmd_target_invalid)),
            )
        val host = target.first
        val port = target.second
        return if (connectTcp(host, port)) {
            Result.success(target)
        } else {
            Result.failure(
                IllegalStateException(
                    context.getString(R.string.activation_cmd_connect_failed, host, port),
                ),
            )
        }
    }

    /** Extract host/port from an `adb connect` command or a raw `host:port` string. */
    fun parseAdbTarget(command: String): Pair<String, Int>? {
        val cleaned = command.trim()
        if (cleaned.isEmpty()) return null
        val withoutPrefix = cleaned
            .removePrefix("adb")
            .trim()
            .removePrefix("connect")
            .trim()
            .trim('"', '\'')
        val candidate = withoutPrefix.split(" ").lastOrNull()?.trim() ?: return null
        val idx = candidate.lastIndexOf(':')
        if (idx <= 0 || idx == candidate.length - 1) return null
        val host = candidate.substring(0, idx)
        val port = candidate.substring(idx + 1).toIntOrNull() ?: return null
        if (host.isEmpty() || port !in 1..65535) return null
        return host to port
    }

    /** Ask adbd to listen on a fixed TCP port so future activations skip pairing. */
    fun enableTcpMode(port: Int = 5555): Boolean {
        val c = currentTlsClient() ?: return false
        return try {
            val ok = c.openTcpPort(port)
            if (ok) prefs.adbPort = port
            ok
        } catch (t: Throwable) {
            Logger.e("enableTcpMode failed: ${t.message}")
            false
        }
    }

    fun verifyShell(): Boolean = runShell("echo ISPOSED_OK")?.contains("ISPOSED_OK") == true

    fun runShell(command: String): String? {
        currentTlsClient()?.let { c ->
            try {
                return c.shell(command)
            } catch (t: Throwable) {
                Logger.w("shell on TLS client failed: ${t.message}")
            }
        }
        currentTcpClient()?.let { c ->
            try {
                return c.shell(command)
            } catch (t: Throwable) {
                Logger.w("shell on TCP client failed: ${t.message}")
            }
        }
        // Fall back to a fresh TCP connection using the persistent ADB key.
        return try {
            AdbClient(prefs.adbHost, prefs.adbPort, keys.key()).use { c ->
                c.connect()
                if (!c.isAuthenticated) null else c.shell(command)
            }
        } catch (t: Throwable) {
            Logger.d("shell on TCP client failed: ${t.message}")
            null
        }
    }

    fun isActive(): Boolean = active

    fun activeHost(): String? = if (active) prefs.adbHost else null

    fun activePort(): Int = prefs.adbPort

    fun deactivate() {
        closeClient()
        active = false
        prefs.adbPaired = false
    }

    private fun currentTlsClient(): AdbTlsClient? = synchronized(lock) { tlsClient }

    private fun currentTcpClient(): AdbClient? = synchronized(lock) { tcpClient }

    private fun closeClient() {
        synchronized(lock) {
            try { tlsClient?.close() } catch (_: Throwable) {}
            try { tcpClient?.close() } catch (_: Throwable) {}
            tlsClient = null
            tcpClient = null
        }
    }
}
