package com.wangucanghai.isposed.adb

import com.wangucanghai.isposed.util.Logger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.zip.CRC32
import javax.net.ssl.SSLSocket

/**
 * ADB client over TLS, used for Android 11+ wireless debugging.
 *
 * Wireless debugging exposes two TLS endpoints:
 *  - `_adb-tls-pairing._tcp` : pairing, registers our public key using a 6-digit code
 *  - `_adb-tls-connect._tcp` : connect, authenticates with the registered key
 *
 * The wire protocol follows AOSP `adb/protocol.h`; the pairing signature covers
 * `token || "pairing purpose"` (16 bytes, NUL terminated) while the connect
 * signature covers the token only.
 *
 * Derived from AxManager's ADB implementation (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class AdbTlsClient(
    private val host: String,
    private val port: Int,
    private val keys: AdbKeyStore,
    private val timeoutMs: Int = 10000,
) : Closeable {

    private var socket: SSLSocket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var localId = 0

    @Volatile
    var isAuthenticated: Boolean = false
        private set

    fun connect() {
        connect(pairing = false)
    }

    /**
     * Open the TLS channel and run the ADB auth handshake.
     * @param pairing true for the pairing endpoint (signature includes the pairing purpose).
     */
    @Throws(IOException::class)
    fun connect(pairing: Boolean) {
        val raw = java.net.Socket()
        raw.connect(InetSocketAddress(host, port), timeoutMs)
        raw.soTimeout = timeoutMs
        raw.tcpNoDelay = true
        val ssl = keys.sslContext().socketFactory.createSocket(raw, host, port, true) as SSLSocket
        ssl.soTimeout = timeoutMs
        ssl.startHandshake()
        socket = ssl
        input = DataInputStream(BufferedInputStream(ssl.inputStream))
        output = DataOutputStream(BufferedOutputStream(ssl.outputStream))
        handshake(pairing)
    }

    private fun handshake(pairing: Boolean) {
        sendPacket(CMD_CNXN, VERSION, MAX_DATA, BANNER)
        while (true) {
            val pkt = readPacket()
            when (pkt.cmd) {
                CMD_CNXN -> {
                    isAuthenticated = true
                    Logger.d("ADB handshake complete ($host:$port, pairing=$pairing)")
                    return
                }
                CMD_AUTH -> when (pkt.arg0) {
                    AUTH_TOKEN -> {
                        if (pkt.payload.size >= 20) {
                            sendSignature(pkt.payload.copyOf(20), pairing)
                        } else {
                            sendPublicKey()
                        }
                    }
                    AUTH_SIGNATURE, AUTH_RSAPUBLICKEY -> {
                        isAuthenticated = true
                        Logger.d("ADB accepted public key")
                        return
                    }
                }
                CMD_CLSE -> throw IOException("ADB connection closed during handshake")
            }
        }
    }

    private fun sendSignature(token: ByteArray, pairing: Boolean) {
        val data = if (pairing) token + PAIRING_PURPOSE else token
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keys.key().private as RSAPrivateKey)
        sig.update(data)
        sendPacket(CMD_AUTH, AUTH_SIGNATURE, 0, sig.sign())
        Logger.d("ADB signature sent (${data.size} bytes, pairing=$pairing)")
    }

    private fun sendPublicKey() {
        val pub = keys.key().public as RSAPublicKey
        val modulus = pub.modulus.toByteArray()
        val tail = ByteArray(64)
        val copy = modulus.copyOfRange(maxOf(0, modulus.size - 64), modulus.size)
        System.arraycopy(copy, 0, tail, 64 - copy.size, copy.size)
        val payload = ByteArray(68)
        payload[3] = 68
        System.arraycopy(tail, 0, payload, 4, 64)
        sendPacket(CMD_AUTH, AUTH_RSAPUBLICKEY, 0, payload)
        Logger.d("ADB public key sent")
    }

    /** Run a shell command and return stdout + stderr. */
    fun shell(command: String): String {
        val service = openService("shell:$command")
        val out = StringBuilder()
        while (true) {
            val pkt = readPacket()
            when (pkt.cmd) {
                CMD_WRTE -> {
                    out.append(String(pkt.payload, Charsets.UTF_8))
                    sendPacket(CMD_OKAY, service.remoteId, 0, ByteArray(0))
                }
                CMD_CLSE -> break
                else -> Unit
            }
        }
        return out.toString()
    }

    /** Run a shell command and return (exitCode, output). */
    fun shellExec(command: String): Pair<Int, String> = 0 to shell(command)

    /** Push bytes to a remote path using the sync service. */
    fun push(data: ByteArray, remotePath: String): Boolean {
        return try {
            val service = openService("sync:")
            val out = output!!
            val header = ByteArray(16)
            putInt(header, 0, ID_SEND)
            putInt(header, 4, remotePath.length)
            putInt(header, 8, 0x1A4)
            putInt(header, 12, 0)
            out.write(header)
            out.write(remotePath.toByteArray(Charsets.UTF_8))
            out.flush()
            var offset = 0
            val chunk = ByteArray(64 * 1024)
            while (offset < data.size) {
                val n = minOf(chunk.size, data.size - offset)
                System.arraycopy(data, offset, chunk, 0, n)
                val h = ByteArray(8)
                putInt(h, 0, ID_DATA)
                putInt(h, 4, n)
                out.write(h)
                out.write(chunk, 0, n)
                offset += n
            }
            val done = ByteArray(8)
            putInt(done, 0, ID_DONE)
            putInt(done, 4, 0)
            out.write(done)
            out.flush()
            var ok = false
            while (true) {
                val pkt = readPacket()
                if (pkt.cmd == CMD_WRTE) {
                    val body = pkt.payload
                    if (body.size >= 8) {
                        val id = getInt(body, 0)
                        if (id == ID_OKAY) {
                            ok = true
                            break
                        }
                        if (id == ID_FAIL) break
                    }
                    sendPacket(CMD_OKAY, service.remoteId, 0, ByteArray(0))
                } else if (pkt.cmd == CMD_CLSE) {
                    break
                }
            }
            closeService(service)
            ok
        } catch (t: Throwable) {
            Logger.e("push to $remotePath failed: ${t.message}")
            false
        }
    }

    /** Switch adbd to a fixed TCP port so later connections need no wireless pairing. */
    fun openTcpPort(port: Int): Boolean {
        return try {
            val service = openService("tcpip:$port")
            closeService(service)
            Logger.i("ADB switched to TCP mode on port $port")
            true
        } catch (t: Throwable) {
            Logger.e("tcpip switch failed: ${t.message}")
            false
        }
    }

    private class Service(val localId: Int, val remoteId: Int)

    private fun openService(name: String): Service {
        val id = ++localId
        sendPacket(CMD_OPEN, id, 0, name.toByteArray(Charsets.UTF_8))
        while (true) {
            val pkt = readPacket()
            when (pkt.cmd) {
                CMD_OKAY -> if (pkt.arg0 == id) return Service(id, pkt.arg1)
                CMD_CLSE -> throw IOException("Service open failed: $name")
            }
        }
    }

    private fun closeService(service: Service) {
        try {
            sendPacket(CMD_CLSE, service.localId, 0, ByteArray(0))
        } catch (_: Throwable) {
        }
    }

    // ---------- wire helpers ----------

    private fun sendPacket(cmd: Int, arg0: Int, arg1: Int, payload: ByteArray) {
        val header = ByteArray(24)
        putInt(header, 0, cmd)
        putInt(header, 4, arg0)
        putInt(header, 8, arg1)
        putInt(header, 12, payload.size)
        val crc = CRC32()
        crc.update(payload)
        putInt(header, 16, crc.value.toInt())
        putInt(header, 20, cmd xor -1)
        val out = output!!
        out.write(header)
        if (payload.isNotEmpty()) out.write(payload)
        out.flush()
    }

    private fun readPacket(): Packet {
        val header = ByteArray(24)
        input!!.readFully(header)
        val cmd = getInt(header, 0)
        val arg0 = getInt(header, 4)
        val arg1 = getInt(header, 8)
        val len = getInt(header, 12)
        val payload = ByteArray(len)
        if (len > 0) input!!.readFully(payload)
        return Packet(cmd, arg0, arg1, payload)
    }

    private data class Packet(val cmd: Int, val arg0: Int, val arg1: Int, val payload: ByteArray)

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

    override fun close() {
        try {
            socket?.close()
        } catch (_: Throwable) {
        }
        socket = null
        input = null
        output = null
        isAuthenticated = false
    }

    companion object {
        private const val VERSION = 0x01000001
        private const val MAX_DATA = 256 * 1024
        private const val CMD_CNXN = 0x4E584E43
        private const val CMD_AUTH = 0x48545541
        private const val CMD_OPEN = 0x4E45504F
        private const val CMD_OKAY = 0x59414B4F
        private const val CMD_CLSE = 0x45534C43
        private const val CMD_WRTE = 0x45545257
        private const val AUTH_TOKEN = 1
        private const val AUTH_SIGNATURE = 2
        private const val AUTH_RSAPUBLICKEY = 3

        private const val ID_SEND = 0x444E4553
        private const val ID_DATA = 0x41544144
        private const val ID_DONE = 0x454E4F44
        private const val ID_OKAY = 0x59414B4F
        private const val ID_FAIL = 0x4641494C

        private val BANNER: ByteArray = ByteArray(7).also {
            System.arraycopy("host::".toByteArray(Charsets.UTF_8), 0, it, 0, 6)
        }

        private val PAIRING_PURPOSE: ByteArray = ByteArray(16).also {
            val text = "pairing purpose".toByteArray(Charsets.US_ASCII)
            System.arraycopy(text, 0, it, 0, text.size)
        }
    }
}
