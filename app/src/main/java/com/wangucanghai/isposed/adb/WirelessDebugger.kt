package com.wangucanghai.isposed.adb

import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.Signature
import java.security.interfaces.RSAPrivateKey

/**
 * Orchestrates root-free activation over wireless debugging (ADB over TCP).
 *
 * 1. [pair]: register our RSA public key with the device via the pairing port
 *    (Android 11+ wireless debugging pairing, 6-digit code).
 * 2. [activate]: connect to the ADB port with the now-authorized key and verify
 *    shell access, then keep the service alive.
 *
 * Derived from AxManager's wireless activation (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class WirelessDebugger(private val prefs: AppPrefs) {

    /**
     * Pair with the device using the wireless debugging pairing port + 6-digit code.
     * On success the device remembers our public key, enabling subsequent connects.
     */
    fun pair(host: String, pairPort: Int, code: String): Boolean {
        val key = KeyStore.loadOrCreate()
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, pairPort), 8000)
                s.soTimeout = 8000
                val input = DataInputStream(s.getInputStream())
                val output = DataOutputStream(s.getOutputStream())
                // read server CNXN (payload "pairing:<guid>")
                val cnxn = readPacket(input)
                val payload = String(cnxn.third, Charsets.UTF_8)
                Logger.i("Pairing handshake: ${payload.trimEnd('\u0000')}")
                // client sends AUTH TOKEN (empty)
                sendPacket(output, CMD_AUTH, AUTH_TOKEN, ByteArray(0))
                // server sends AUTH with pairing hash payload
                val auth = readPacket(input)
                if (auth.first != CMD_AUTH) return false
                val hash = auth.third
                if (hash.isEmpty()) return false
                // sign hash[0:4] + hash  (pairing purpose)
                val purpose = ByteArray(16) { 'P'.code.toByte() } // "pairing purpose" region
                val toSign = ByteArray(4 + hash.size)
                System.arraycopy(hash, 0, toSign, 0, 4)
                System.arraycopy(hash, 0, toSign, 4, hash.size)
                val sig = Signature.getInstance("SHA1withRSA")
                sig.initSign(key.private as RSAPrivateKey)
                sig.update(toSign)
                sendPacket(output, CMD_AUTH, AUTH_SIGNATURE, sig.sign())
                // expect CNXN
                val res = readPacket(input)
                val ok = res.first == CMD_CNXN
                if (ok) {
                    prefs.adbHost = host
                    prefs.adbPort = ConstantsPort.ADB_DEFAULT
                    prefs.adbPaired = true
                    Logger.i("Wireless pairing OK")
                }
                ok
            }
        } catch (t: Throwable) {
            Logger.e("Pairing failed: ${t.message}")
            false
        }
    }

    /** Connect & verify shell access on the ADB port. */
    fun activate(host: String, port: Int): Boolean {
        val key = KeyStore.loadOrCreate()
        return try {
            AdbClient(host, port, key).use { client ->
                client.connect()
                if (!client.isAuthenticated) {
                    Logger.e("ADB not authenticated - pair first")
                    return false
                }
                val out = client.shell("echo ISPOSED_OK && id")
                prefs.adbHost = host
                prefs.adbPort = port
                prefs.adbPaired = true
                Logger.i("Activated: $out")
                true
            }
        } catch (t: Throwable) {
            Logger.e("Activation failed: ${t.message}")
            false
        }
    }

    private fun readPacket(input: DataInputStream): Triple<Int, Int, ByteArray> {
        val header = ByteArray(24)
        input.readFully(header)
        val cmd = getInt(header, 0)
        val arg0 = getInt(header, 4)
        val len = getInt(header, 12)
        val payload = ByteArray(len)
        if (len > 0) input.readFully(payload)
        return Triple(cmd, arg0, payload)
    }

    private fun sendPacket(out: DataOutputStream, cmd: Int, arg0: Int, payload: ByteArray) {
        val header = ByteArray(24)
        putInt(header, 0, cmd)
        putInt(header, 4, arg0)
        putInt(header, 8, 0)
        putInt(header, 12, payload.size)
        putInt(header, 16, 0)
        putInt(header, 20, cmd xor -1)
        out.write(header)
        if (payload.isNotEmpty()) out.write(payload)
        out.flush()
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

    private object ConstantsPort { const val ADB_DEFAULT = 5555 }

    companion object {
        private const val CMD_AUTH = 0x48545541
        private const val CMD_CNXN = 0x4E584E43
        private const val AUTH_TOKEN = 1
        private const val AUTH_SIGNATURE = 2
    }
}
