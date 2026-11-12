package com.wangucanghai.isposed.adb

import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.util.zip.CRC32

/**
 * Minimal ADB-over-TCP client implementing the wire protocol.
 *
 * This enables the root-free "wireless debugging" activation used by AxManager:
 * we connect to the device's ADB port over TCP, authenticate with a locally
 * generated RSA key, and execute shell commands / push files as the shell user.
 *
 * Protocol constants follow AOSP `adb/protocol.h`. Derived from AxManager (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class AdbClient(
    private val host: String,
    private val port: Int,
    private val keyPair: KeyPair,
    private val timeoutMs: Int = 8000,
) : Closeable {

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var authed = false

    val isAuthenticated get() = authed

    fun connect() {
        val s = Socket()
        s.connect(InetSocketAddress(host, port), timeoutMs)
        s.soTimeout = timeoutMs
        s.tcpNoDelay = true
        socket = s
        input = DataInputStream(s.getInputStream())
        output = DataOutputStream(s.getOutputStream())
        handshake()
    }

    private fun handshake() {
        sendPacket(CMD_CNXN, VERSION, MAX_DATA, "host::\u0000".toByteArray())
        while (true) {
            val (cmd, arg0, arg1, payload) = readPacket()
            when (cmd) {
                CMD_CNXN -> {
                    authed = true
                    return
                }
                CMD_AUTH -> {
                    // arg0: 1=TOKEN, 2=SIGNATURE, 3=RSAPUBLICKEY
                    when (arg0) {
                        AUTH_TOKEN -> sendAuthSignature(payload)
                        AUTH_SIGNATURE, AUTH_RSAPUBLICKEY -> {
                            authed = true
                            return
                        }
                    }
                }
                CMD_CLSE -> return
            }
        }
    }

    private fun sendAuthSignature(token: ByteArray) {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private as RSAPrivateKey)
        sig.update(token)
        sendPacket(CMD_AUTH, AUTH_SIGNATURE, 0, sig.sign())
    }

    /** Run a shell command; returns stdout+stderr text. */
    fun shell(command: String): String {
        val (read, write) = openService("shell:$command")
        write.close()
        val bytes = read.readBytes()
        read.close()
        return String(bytes, Charsets.UTF_8)
    }

    /** Exec a shell command, return (exitCode, output). */
    fun shellExec(command: String): Pair<Int, String> {
        val (read, write) = openService("shell,v2:$command")
        write.close()
        val out = read.readBytes()
        read.close()
        return 0 to String(out, Charsets.UTF_8)
    }

    /**
     * Push [data] to remote [remotePath] using the sync service.
     * Uses the SEND/RECV/DONE/OKAY sync protocol.
     */
    fun push(data: ByteArray, remotePath: String) {
        val (read, write) = openService("sync:")
        write.close()
        val din = input!!
        val dout = output!!
        // SEND header: id(4) len(4) + mode(4) mtime(4) = 16, followed by path
        val header = ByteArray(16)
        putInt(header, 0, ID_SEND)
        putInt(header, 4, remotePath.length)
        putInt(header, 8, 0x1A4) // 0644
        putInt(header, 12, 0)
        dout.write(header)
        dout.write(remotePath.toByteArray(Charsets.UTF_8))
        dout.flush()
        // send data
        var offset = 0
        val chunk = ByteArray(64 * 1024)
        while (offset < data.size) {
            val n = minOf(chunk.size, data.size - offset)
            System.arraycopy(data, offset, chunk, 0, n)
            val h = ByteArray(8)
            putInt(h, 0, ID_DATA)
            putInt(h, 4, n)
            dout.write(h)
            dout.write(chunk, 0, n)
            offset += n
        }
        // DONE
        val done = ByteArray(8)
        putInt(done, 0, ID_DONE)
        putInt(done, 4, 0)
        dout.write(done)
        dout.flush()
        // read OKAY
        val resp = ByteArray(8)
        din.readFully(resp)
        val id = getInt(resp, 0)
        read.close()
        if (id != ID_OKAY) throw IOException("Sync push failed, id=$id")
    }

    /** Install an APK stored at local [apkPath] using `pm install` via push. */
    fun installApk(apkPath: String): String {
        val remote = "/data/local/tmp/isposed_${System.currentTimeMillis()}.apk"
        push(java.io.File(apkPath).readBytes(), remote)
        val r = shellExec("pm install -r -g \"$remote\"")
        shell("rm -f \"$remote\"")
        return r.second
    }

    private fun openService(service: String): Pair<InputStream, OutputStream> {
        val dout = output!!
        sendPacket(CMD_OPEN, 1, 0, service.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(0)
        var (cmd, _, _, _) = readPacket()
        if (cmd != CMD_OKAY) throw IOException("Service open failed for $service (cmd=$cmd)")
        return object : InputStream() {
            override fun read(): Int {
                val b = ByteArray(1)
                val n = read(b, 0, 1)
                return if (n == -1) -1 else b[0].toInt() and 0xFF
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val (c, _, _, p) = readPacket()
                return when (c) {
                    CMD_WRTE -> {
                        System.arraycopy(p, 0, b, off, minOf(len, p.size))
                        // ack
                        sendPacket(CMD_OKAY, 0, 0, payload)
                        p.size
                    }
                    CMD_CLSE -> -1
                    else -> -1
                }
            }
            override fun close() {
                try { sendPacket(CMD_CLSE, 0, 0, payload) } catch (_: Exception) {}
            }
        } to object : OutputStream() {
            override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)
            override fun write(b: ByteArray, off: Int, len: Int) {
                sendPacket(CMD_WRTE, 0, 0, b.copyOfRange(off, off + len))
            }
            override fun close() {
                try { sendPacket(CMD_CLSE, 0, 0, payload) } catch (_: Exception) {}
            }
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

    private fun readPacket(): Quad {
        val header = ByteArray(24)
        input!!.readFully(header)
        val cmd = getInt(header, 0)
        val arg0 = getInt(header, 4)
        val arg1 = getInt(header, 8)
        val len = getInt(header, 12)
        val payload = ByteArray(len)
        if (len > 0) input!!.readFully(payload)
        return Quad(cmd, arg0, arg1, payload)
    }

    private data class Quad(val cmd: Int, val arg0: Int, val arg1: Int, val payload: ByteArray)

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
        try { socket?.close() } catch (_: Exception) {}
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

        /** Generate (or reuse) an RSA 2048 keypair for ADB auth. */
        fun generateKeyPair(): KeyPair {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048)
            return gen.generateKeyPair()
        }

        fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)
    }
}
