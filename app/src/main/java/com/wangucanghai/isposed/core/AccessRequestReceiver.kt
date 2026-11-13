package com.wangucanghai.isposed.core

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.wangucanghai.isposed.ui.MainActivity
import com.wangucanghai.isposed.util.Logger

/**
 * Accepts authorization requests from other applications on the device.
 *
 * A requesting app sends [AuthorizationManager.ACTION_REQUEST_ACCESS] with its
 * own package name. This receiver validates the requester is really installed,
 * remembers the request and brings the application to the foreground so the
 * user decides. Declining or allowing writes the result back to the prefs that
 * the requester can query through [QueryAccessReceiver].
 *
 * Copyright (C) 2026 万古沧海
 */
class AccessRequestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AuthorizationManager.ACTION_REQUEST_ACCESS) return
        val requester = intent.getStringExtra(AuthorizationManager.EXTRA_REQUESTER).orEmpty()
        val pm = context.packageManager
        val info = runCatching {
            pm.getApplicationInfo(requester, PackageManager.GET_META_DATA)
        }.getOrNull() ?: run {
            Logger.w("access request from an unknown package, ignored")
            return
        }
        val level = intent.getStringExtra(AuthorizationManager.EXTRA_LEVEL)
            ?.let { raw -> runCatching { AuthLevel.valueOf(raw) }.getOrNull() }
            ?: AuthLevel.READ

        val prefs = AppPrefs(context)
        prefs.pendingRequester = info.packageName
        prefs.pendingRequestTitle = pm.getApplicationLabel(info).toString()
        prefs.pendingRequestLevel = level.name

        val main = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AuthorizationManager.KEY_REQUEST_PENDING, true)
        runCatching { context.startActivity(main) }
            .onFailure { Logger.w("cannot open the app for the access request: ${it.message}") }
    }
}

/**
 * Lets another application read the authorization state of a package without
 * showing any UI. The requester supplies the target package name and reads the
 * result from the broadcast result extras.
 */
class QueryAccessReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AuthorizationManager.ACTION_QUERY_ACCESS) return
        val target = intent.getStringExtra(AuthorizationManager.EXTRA_REQUESTER).orEmpty()
        val manager = AuthorizationManager(context)
        val level = manager.levelOf(target)
        val result = Bundle().apply {
            putBoolean(AuthorizationManager.EXTRA_GRANTED, level != null)
            putString(AuthorizationManager.EXTRA_LEVEL_NAME, level?.name)
        }
        setResult(Activity.RESULT_OK, null, result)
    }
}
