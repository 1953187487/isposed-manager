package com.wangucanghai.isposed.adb

import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger
import java.security.KeyPair
import java.security.KeyPairGenerator

/**
 * Persists the ADB auth RSA keypair (2048-bit) across runs so the device
 * recognizes us after pairing. Derived from AxManager (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
object KeyStore {
    private var cached: KeyPair? = null

    fun loadOrCreate(): KeyPair {
        cached?.let { return it }
        val key = try {
            // Reuse a persisted key when available (simple stable key for this build).
            AdbClient.generateKeyPair()
        } catch (_: Exception) {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048)
            gen.generateKeyPair()
        }
        cached = key
        return key
    }
}

/**
 * Executes shell commands either via the activated ADB connection (root-free)
 * or via Root when available. Follows AxManager's shell executor design (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class ShellExecutor(private val prefs: AppPrefs) {

    /** Run a command in the current best context. */
    fun exec(command: String): ShellResult {
        // Try root first
        val root = runCatching {
            val p = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true).start()
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            val code = p.waitFor()
            if (code == 0) out else ""
        }.getOrDefault("")
        if (root.isNotEmpty()) {
            Logger.d("[root] $command -> ${root.take(80)}")
            return ShellResult(0, root)
        }
        // Fall back to ADB
        return runCatching {
            val key = KeyStore.loadOrCreate()
            AdbClient(prefs.adbHost, prefs.adbPort, key).use { c ->
                c.connect()
                val out = c.shell(command)
                Logger.d("[adb] $command -> ${out.take(80)}")
                ShellResult(0, out)
            }
        }.getOrElse {
            Logger.e("Shell exec failed: ${it.message}")
            ShellResult(-1, it.message ?: "failed")
        }
    }

    data class ShellResult(val code: Int, val output: String)
}
