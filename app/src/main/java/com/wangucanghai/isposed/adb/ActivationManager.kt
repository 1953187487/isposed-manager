package com.wangucanghai.isposed.adb

import android.content.Context
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger

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
    private var client: AdbTlsClient? = null

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

    /** Connect to the device's connect endpoint and keep the channel for shell use. */
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
                    client = c
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

    /** Connect directly to a known TCP port (e.g. fixed 5555 after tcpip switch). */
    fun connectTcp(host: String, port: Int): Boolean {
        closeClient()
        val c = AdbTlsClient(host, port, keys)
        return try {
            c.connect(pairing = false)
            if (!c.isAuthenticated) return false
            val out = c.shell("echo ISPOSED_OK && id")
            if (out.contains("ISPOSED_OK")) {
                synchronized(lock) {
                    client = c
                    active = true
                }
                prefs.adbHost = host
                prefs.adbPort = port
                prefs.adbPaired = true
                true
            } else {
                c.close()
                false
            }
        } catch (t: Throwable) {
            Logger.e("connectTcp failed: ${t.message}")
            c.close()
            false
        }
    }

    /** Ask adbd to listen on a fixed TCP port so future activations skip pairing. */
    fun enableTcpMode(port: Int = 5555): Boolean {
        val c = currentClient() ?: return false
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
        currentClient()?.let { c ->
            return try {
                c.shell(command)
            } catch (t: Throwable) {
                Logger.w("shell on TLS client failed: ${t.message}")
                null
            }
        }
        // Fall back to a fresh TCP connection (fixed-port mode).
        return try {
            AdbClient(prefs.adbHost, prefs.adbPort, AdbClient.generateKeyPair()).use { c ->
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

    private fun currentClient(): AdbTlsClient? = synchronized(lock) { client }

    private fun closeClient() {
        synchronized(lock) {
            client?.close()
            client = null
        }
    }
}
