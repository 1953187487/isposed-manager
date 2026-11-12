package com.wangucanghai.isposed.util

import android.util.Log

/**
 * In-memory log buffer + logcat mirror. Used by the Logs page.
 *
 * Copyright (C) 2026 万古沧海
 */
object Logger {
    const val TAG = "IsposedManager"
    private val buffer = ArrayDeque<String>()
    private const val MAX = 2000

    @Synchronized
    fun d(msg: String) {
        Log.d(TAG, msg)
        push("[${ts()}] D  $msg")
    }

    @Synchronized
    fun i(msg: String) {
        Log.i(TAG, msg)
        push("[${ts()}] I  $msg")
    }

    @Synchronized
    fun w(msg: String) {
        Log.w(TAG, msg)
        push("[${ts()}] W  $msg")
    }

    @Synchronized
    fun e(msg: String) {
        Log.e(TAG, msg)
        push("[${ts()}] E  $msg")
    }

    private fun ts(): String =
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())

    private fun push(line: String) {
        buffer.addLast(line)
        while (buffer.size > MAX) buffer.removeFirst()
    }

    @Synchronized
    fun entries(): List<String> = buffer.toList()

    @Synchronized
    fun clear() = buffer.clear()
}
