package com.wangucanghai.isposed.adb

import android.content.Context
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger

/**
 * Executes shell commands either via the activated ADB connection (root-free)
 * or via Root when available. Follows AxManager's shell executor design (Apache-2.0).
 *
 * The ADB fallback authenticates with the persistent identity from
 * [AdbKeyStore], the same key registered on the device during wireless
 * pairing, so previously authorized devices keep working.
 *
 * Copyright (C) 2026 万古沧海
 */
class ShellExecutor(private val context: Context, private val prefs: AppPrefs) {

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
        // Fall back to ADB using the persistent identity
        return runCatching {
            val key = AdbKeyStore(context).key()
            AdbClient(prefs.adbHost, prefs.adbPort, key).use { c ->
                c.connect()
                if (!c.isAuthenticated) {
                    return@runCatching ShellResult(
                        -1,
                        context.getString(R.string.shell_not_authorized),
                    )
                }
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
