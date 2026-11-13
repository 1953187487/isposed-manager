package com.wangucanghai.isposed.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.wangucanghai.isposed.util.Logger
import java.net.Inet4Address

/**
 * Local network discovery helpers.
 *
 * The previous implementation assumed a fixed `/24` subnet and probed every
 * interface, including cellular ones. On a real LAN the prefix length is
 * frequently `/16` or `/23`, so devices outside the assumed block were never
 * reached. This helper reads the actual prefix length from the platform
 * network stack and keeps only Wi-Fi and Ethernet transports, which are the
 * networks that carry ADB over TCP.
 *
 * Copyright (C) 2026 万古沧海
 */
object NetworkDiscovery {

    /** Upper bound for the number of probed addresses per interface. */
    const val MAX_HOSTS_PER_NETWORK = 1024

    /**
     * One usable IPv4 network.
     *
     * @param interfaceName name of the owning network interface
     * @param ipv4 local address of this device on that interface
     * @param prefixLength CIDR prefix length reported by the platform
     * @param subnetBase network address with the host bits cleared
     * @param hostCount number of addresses that will be probed
     */
    data class LocalNetwork(
        val interfaceName: String,
        val ipv4: String,
        val prefixLength: Int,
        val subnetBase: String,
        val hostCount: Int,
    )

    /**
     * Collect the Wi-Fi and Ethernet networks this device is attached to.
     *
     * @return the usable networks, empty when the device runs on a cellular or
     *         virtual interface only
     */
    fun activeLocalNetworks(context: Context): List<LocalNetwork> {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        val result = mutableListOf<LocalNetwork>()
        try {
            cm.allNetworks.forEach { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@forEach
                val local = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                if (!local) return@forEach
                val props = cm.getLinkProperties(network) ?: return@forEach
                val ifName = props.interfaceName ?: return@forEach
                props.linkAddresses.forEach { link ->
                    val address = link.address as? Inet4Address ?: return@forEach
                    val prefix = link.prefixLength.coerceIn(8, 30)
                    result += LocalNetwork(
                        interfaceName = ifName,
                        ipv4 = address.hostAddress ?: return@forEach,
                        prefixLength = prefix,
                        subnetBase = subnetBase(address, prefix),
                        hostCount = hostCount(prefix),
                    )
                }
            }
        } catch (t: Throwable) {
            Logger.w("NetworkDiscovery failed: ${t.message}")
        }
        return result.distinctBy { "${it.interfaceName}:${it.subnetBase}" }
    }

    /**
     * Enumerate every address to probe for the given networks. The local
     * address of every interface is skipped.
     */
    fun candidateHosts(networks: List<LocalNetwork>): List<String> {
        val own = networks.map { it.ipv4 }.toSet()
        val hosts = linkedSetOf<String>()
        for (network in networks) {
            val base = toInt(network.subnetBase) ?: continue
            for (offset in 1..network.hostCount) {
                val candidate = toAddress(base + offset)
                if (candidate in own) continue
                hosts += candidate
            }
        }
        return hosts.toList()
    }

    private fun toInt(address: String): Int? {
        val parts = address.split('.')
        if (parts.size != 4) return null
        var value = 0
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            value = (value shl 8) or octet
        }
        return value
    }

    private fun toAddress(value: Int): String =
        "${(value shr 24) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 8) and 0xFF}.${value and 0xFF}"

    /** Number of usable host addresses for a prefix length. */
    fun hostCount(prefixLength: Int): Int {
        val total = 1 shl (32 - prefixLength.coerceIn(8, 30))
        val usable = (total - 2).coerceAtLeast(1)
        return usable.coerceAtMost(MAX_HOSTS_PER_NETWORK)
    }

    /** Network address of [address] under [prefixLength]. */
    fun subnetBase(address: Inet4Address, prefixLength: Int): String {
        val bytes = address.address
        val mask = prefixToMask(prefixLength)
        return (0 until 4).joinToString(".") { index ->
            ((bytes[index].toInt() and 0xFF) and (mask[index].toInt() and 0xFF)).toString()
        }
    }

    private fun prefixToMask(prefixLength: Int): IntArray {
        val mask = IntArray(4) { 0xFF }
        var remaining = prefixLength.coerceIn(0, 32)
        for (index in 0 until 4) {
            if (remaining >= 8) {
                remaining -= 8
                continue
            }
            mask[index] = if (remaining == 0) 0 else (0xFF shl (8 - remaining)) and 0xFF
            remaining = 0
        }
        return mask
    }

    /** Human readable summary used by the discovery log. */
    fun describe(networks: List<LocalNetwork>): String =
        networks.joinToString("；") { "${it.interfaceName} ${it.ipv4}/$it.prefixLength (${it.subnetBase})" }
}
