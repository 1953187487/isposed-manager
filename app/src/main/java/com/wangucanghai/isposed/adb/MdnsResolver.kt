package com.wangucanghai.isposed.adb

import com.wangucanghai.isposed.util.Logger
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

/**
 * mDNS resolver for Android's ADB wireless debugging services.
 *
 * Discovers `_adb-tls-pairing._tcp.local` and `_adb-tls-connect._tcp.local`
 * so the user never has to type an IP or port manually. The same discovery
 * AxManager uses for its wireless-debugging activation.
 *
 * Copyright (C) 2026 万古沧海
 */
class MdnsResolver {

    data class AdbService(
        val instanceName: String,
        val host: String,
        val port: Int,
        val type: String,
    )

    companion object {
        const val TYPE_PAIRING = "_adb-tls-pairing"
        const val TYPE_CONNECT = "_adb-tls-connect"

        private const val MDNS_ADDR = "224.0.0.251"
        private const val MDNS_PORT = 5353
        private const val TYPE_PTR = 12
        private const val TYPE_SRV = 33
        private const val TYPE_A = 1
        private const val CLASS_IN = 1
        private const val MAX_PACKETS = 64
    }

    fun discover(type: String, timeoutMs: Int = 4000): List<AdbService> {
        val query = buildQuery(type)
        val answers = queryMdns(query, timeoutMs)
        return buildServices(answers, type)
    }

    fun discoverAll(timeoutMs: Int = 4000): Pair<List<AdbService>, List<AdbService>> =
        discover(TYPE_PAIRING, timeoutMs) to discover(TYPE_CONNECT, timeoutMs)

    private fun buildQuery(type: String): ByteArray {
        val qname = encodeName("$type._tcp.local")
        val out = java.io.ByteArrayOutputStream()
        out.write(byteArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        out.write(qname)
        out.write(byteArrayOf(0, TYPE_PTR.toByte(), 0, CLASS_IN.toByte()))
        return out.toByteArray()
    }

    private fun queryMdns(query: ByteArray, timeoutMs: Int): List<Answer> {
        val answers = mutableListOf<Answer>()
        val seen = mutableSetOf<String>()
        try {
            MulticastSocket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.reuseAddress = true
                socket.timeToLive = 255
                val group = InetAddress.getByName(MDNS_ADDR)
                socket.joinGroup(group)
                val packet = DatagramPacket(query, query.size, group, MDNS_PORT)
                socket.send(packet)
                val end = System.currentTimeMillis() + timeoutMs
                var packets = 0
                while (System.currentTimeMillis() < end && packets < MAX_PACKETS) {
                    val buf = ByteArray(4096)
                    val dp = DatagramPacket(buf, buf.size)
                    try {
                        socket.receive(dp)
                    } catch (_: java.net.SocketTimeoutException) {
                        break
                    }
                    packets++
                    parseMessage(dp.data, dp.length).forEach { ans ->
                        val key = ans.name + "|" + ans.type + "|" + ans.rd
                        if (seen.add(key)) answers += ans
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.w("mDNS query failed: ${t.message}")
        }
        return answers
    }

    private fun buildServices(answers: List<Answer>, type: String): List<AdbService> {
        // instance -> target host (from SRV)
        val srvHost = mutableMapOf<String, String>()
        val srvPort = mutableMapOf<String, Int>()
        val hostIp = mutableMapOf<String, String>()
        // PTR instance names for our type
        val instances = mutableListOf<String>()
        for (a in answers) {
            when (a.type) {
                TYPE_PTR -> {
                    if (a.name.startsWith(type)) instances += a.rd
                }
                TYPE_SRV -> {
                    srvHost[a.name] = a.rd
                    srvPort[a.name] = a.port
                }
                TYPE_A -> {
                    if (a.name.isNotEmpty()) hostIp[a.name] = a.rd
                }
            }
        }
        val result = mutableListOf<AdbService>()
        for (inst in instances) {
            val target = srvHost[inst] ?: inst
            val port = srvPort[inst] ?: continue
            val ip = hostIp[target] ?: continue
            if (ip.isEmpty()) continue
            result += AdbService(instanceName = inst, host = ip, port = port, type = type)
        }
        return result.distinctBy { it.host to it.port }
    }

    private fun parseMessage(data: ByteArray, length: Int): List<Answer> {
        if (length < 12) return emptyList()
        val ancount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        var off = 12
        // skip question
        repeat(2) {
            val (_, n) = readName(data, off)
            off = n + 4
        }
        val out = mutableListOf<Answer>()
        repeat(ancount) {
            if (off + 10 > length) return@repeat
            val (name, nameEnd) = readName(data, off)
            var p = nameEnd
            if (p + 10 > length) return@repeat
            val rtype = u16(data, p)
            val rclass = u16(data, p + 2)
            val rdlen = u16(data, p + 8)
            p += 10
            if (p + rdlen > length) return@repeat
            val rdata = data.copyOfRange(p, p + rdlen)
            when (rtype) {
                TYPE_PTR -> {
                    val (target, _) = readName(data, p)
                    out += Answer(name, rtype, rclass, target, 0)
                }
                TYPE_SRV -> {
                    val (target, _) = readName(data, p + 6)
                    val port = u16(data, p + 4)
                    out += Answer(name, rtype, rclass, target, port)
                }
                TYPE_A -> {
                    if (rdlen >= 4) {
                        val ip = "${rdata[0].toInt() and 0xFF}.${rdata[1].toInt() and 0xFF}." +
                            "${rdata[2].toInt() and 0xFF}.${rdata[3].toInt() and 0xFF}"
                        out += Answer(name, rtype, rclass, ip, 0)
                    }
                }
            }
            off = p + rdlen
        }
        return out
    }

    private fun readName(data: ByteArray, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var p = start
        var jumped = false
        var end = start
        var guard = 0
        while (guard++ < 64) {
            if (p >= data.size) break
            val len = data[p].toInt() and 0xFF
            if (len == 0) {
                if (!jumped) end = p + 1
                break
            }
            if ((len and 0xC0) == 0xC0) {
                if (p + 1 >= data.size) break
                val ptr = ((len and 0x3F) shl 8) or (data[p + 1].toInt() and 0xFF)
                if (!jumped) end = p + 2
                p = ptr
                jumped = true
            } else {
                if (p + 1 + len > data.size) break
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(String(data, p + 1, len, Charsets.UTF_8))
                p += 1 + len
                if (!jumped) end = p
            }
        }
        return sb.toString() to end
    }

    private fun encodeName(name: String): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (label in name.split('.')) {
            val b = label.toByteArray(Charsets.UTF_8)
            out.write(b.size)
            out.write(b)
        }
        out.write(0)
        return out.toByteArray()
    }

    private fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    private data class Answer(
        val name: String,
        val type: Int,
        val clazz: Int,
        val rd: String,
        val port: Int,
    )
}
