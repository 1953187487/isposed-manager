package com.wangucanghai.isposed.adb

/**
 * A device endpoint that can be reached through an ADB channel.
 *
 * Plain ADB endpoints are found by sweeping the local subnet and probing the
 * well known `adb tcpip` ports. TLS endpoints are the wireless debugging
 * services announced over mDNS; they answer on an ephemeral port and require
 * the TLS handshake instead of the plain CNXN exchange.
 *
 * Copyright (C) 2026 万古沧海
 */
data class DiscoveredDevice(
    val host: String,
    val port: Int,
    val model: String?,
    val tls: Boolean,
    val authorized: Boolean,
    val service: MdnsResolver.AdbService? = null,
) {
    val endpoint: String get() = "$host:$port"

    /** Short badge shown next to the endpoint in the device list. */
    val badge: String
        get() = when {
            tls -> "TLS"
            authorized -> "OK"
            else -> "PENDING"
        }
}
