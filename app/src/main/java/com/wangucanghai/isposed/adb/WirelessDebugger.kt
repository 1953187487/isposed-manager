package com.wangucanghai.isposed.adb

import android.content.Context
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger

/**
 * Root-free activation over wireless debugging (ADB over TLS).
 *
 * Thin facade kept for compatibility; the heavy lifting now lives in
 * [AdbTlsClient] / [ActivationManager] (AxManager-style, Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class WirelessDebugger(private val context: Context, private val prefs: AppPrefs) {

    private val manager = ActivationManager(context)

    /** Pair using the pairing port + 6-digit code (Android 11+). */
    fun pair(host: String, pairPort: Int, code: String): Boolean {
        val svc = MdnsResolver.AdbService("manual", host, pairPort, MdnsResolver.TYPE_PAIRING)
        return manager.pair(svc, code)
    }

    /** Connect & verify shell access on the ADB port. */
    fun activate(host: String, port: Int): Boolean {
        val svc = MdnsResolver.AdbService("manual", host, port, MdnsResolver.TYPE_CONNECT)
        return manager.connect(svc)
    }

    fun isActive(): Boolean = manager.isActive()
}
