package com.wangucanghai.isposed.adb

import com.wangucanghai.isposed.util.Logger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPair
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Plain-ADB-over-TCP LAN discovery.
 *
 * Walks every local IPv4 subnet reported by the platform network stack and
 * probes common ADB TCP ports to find devices that already run `adb tcpip`.
 * Each candidate is confirmed with an ADB CNXN/CNXN-handshake so only real
 * ADB daemons are listed, and the connection banner is parsed to surface the
 * device model. This lets the user pick a device instead of typing its IP
 * manually.
 *
 * Copyright (C) 2026 万古沧海
 */
class LanScanner(private val keyPair: KeyPair? = null) {

    data class FoundDevice(
        val host: String,
        val port: Int,
        val model: String?,
        val authorized: Boolean,
    )

    companion object {
        /** Common plain-ADB-over-TCP ports: 5555 (adb tcpip default) plus the
         *  emulator neighbours seen in the wild. */
        val SCAN_PORTS: List<Int> = listOf(5555, 5554, 5556, 5557, 5558, 5559)
        private const val CONNECT_TIMEOUT_MS = 300
        private const val PROBE_TIMEOUT_MS = 1500
        private const val MAX_PARALLEL = 96
        private const val MAX_SIGN_ATTEMPTS = 3

        private const val VERSION = 0x01000001
        private const val MAX_DATA = 256 * 1024
        private const val CMD_CNXN = 0x4E584E43
        private const val CMD_AUTH = 0x48545541
        private const val AUTH_TOKEN = 1
        private const val AUTH_SIGNATURE = 2
    }

    /**
     * Scan [networks] for [SCAN_PORTS], returning devices that answered an ADB
     * handshake. [onProgress] receives (done, total) probe counts.
     */
    suspend fun scan(
        networks: List<NetworkDiscovery.LocalNetwork>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<FoundDevice> = withContext(Dispatchers.IO) {
        val hosts = NetworkDiscovery.candidateHosts(networks)
        if (hosts.isEmpty()) return@withContext emptyList()

        val targets = buildList {
            for (host in hosts) {
                for (port in SCAN_PORTS) {
                    add(host to port)
                }
            }
        }

        val gate = Semaphore(MAX_PARALLEL)
        val lock = Any()
        val found = mutableListOf<FoundDevice>()
        val counter = AtomicInteger(0)
        val total = targets.size

        coroutineScope {
            val jobs = targets.map { (host, port) ->
                async {
                    gate.withPermit {
                        val device = probe(host, port)
                        if (device != null) {
                            synchronized(lock) { found += device }
                        }
                        val done = counter.incrementAndGet()
                        onProgress(done, total)
                    }
                }
            }
            jobs.awaitAll()
        }
        found.distinctBy { it.host to it.port }.sortedBy { it.host }
    }

    /**
     * Confirm [host]:[port] speaks plain ADB. Sends CNXN and inspects the
     * reply: a CNXN banner proves an ADB daemon, an AUTH challenge means the
     * daemon is present but still needs the key authorized.
     */
    private fun probe(host: String, port: Int): FoundDevice? {
        var socket: Socket? = null
        return try {
            val s = Socket()
            socket = s
            s.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            s.soTimeout = PROBE_TIMEOUT_MS
            s.tcpNoDelay = true
            val input = s.getInputStream()
            val output = s.getOutputStream()
            sendPacket(output, CMD_CNXN, VERSION, MAX_DATA, "host::\u0000".toByteArray())

            val deadline = System.currentTimeMillis() + PROBE_TIMEOUT_MS
            var model: String? = null
            var authorized = false
            var confirmed = false
            var signAttempts = 0

            while (System.currentTimeMillis() < deadline) {
                val remaining = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1)
                s.soTimeout = remaining
                val header = ByteArray(24)
                if (readFully(input, header) < 24) break
                val cmd = getInt(header, 0)
                val arg0 = getInt(header, 4)
                val len = getInt(header, 12)
                val payload = if (len in 1..65536) {
                    val buf = ByteArray(len)
                    if (readFully(input, buf) == len) buf else ByteArray(0)
                } else {
                    ByteArray(0)
                }

                when (cmd) {
                    CMD_CNXN -> {
                        confirmed = true
                        authorized = true
                        model = parseModel(String(payload, Charsets.UTF_8))
                        break
                    }

                    CMD_AUTH -> {
                        confirmed = true
                        if (arg0 == AUTH_TOKEN && keyPair != null && signAttempts < MAX_SIGN_ATTEMPTS) {
                            signAttempts++
                            sendAuthSignature(output, payload)
                            continue
                        }
                        break
                    }

                    else -> return null
                }
            }
            if (confirmed) FoundDevice(host, port, model, authorized) else null
        } catch (t: Throwable) {
            null
        } finally {
            try { socket?.close() } catch (_: Throwable) {}
        }
    }

    private fun parseModel(banner: String): String? {
        if (banner.isBlank()) return null
        val m = Regex("""ro\.product\.model=([^;]+)""").find(banner) ?: return null
        return m.groupValues[1].trim().take(48)
    }

    // ---------- wire helpers ----------

    private fun sendAuthSignature(output: java.io.OutputStream, token: ByteArray) {
        val pair = keyPair ?: return
        try {
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initSign(pair.private as RSAPrivateKey)
            sig.update(token)
            sendPacket(output, CMD_AUTH, AUTH_SIGNATURE, 0, sig.sign())
        } catch (t: Throwable) {
            Logger.w("LanScanner auth signature failed: ${t.message}")
        }
    }

    private fun sendPacket(out: java.io.OutputStream, cmd: Int, arg0: Int, arg1: Int, payload: ByteArray) {
        val header = ByteArray(24)
        putInt(header, 0, cmd)
        putInt(header, 4, arg0)
        putInt(header, 8, arg1)
        putInt(header, 12, payload.size)
        val crc = CRC32()
        crc.update(payload)
        putInt(header, 16, crc.value.toInt())
        putInt(header, 20, cmd xor -1)
        out.write(header)
        if (payload.isNotEmpty()) out.write(payload)
        out.flush()
    }

    private fun readFully(input: java.io.InputStream, buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val n = try {
                input.read(buf, off, buf.size - off)
            } catch (t: Throwable) {
                return off
            }
            if (n <= 0) return off
            off += n
        }
        return off
    }

    private fun putInt(b: ByteArray, off: Int, v: Int) {
        b[off] = (v and 0xFF).toByte()
        b[off + 1] = ((v shr 8) and 0xFF).toByte()
        b[off + 2] = ((v shr 16) and 0xFF).toByte()
        b[off + 3] = ((v shr 24) and 0xFF).toByte()
    }

    private fun getInt(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)
}
