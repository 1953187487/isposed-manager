package com.wangucanghai.isposed.core

import android.content.Context
import android.content.Intent

/**
 * Per-application authorization, modelled after AxManager's per-app grants.
 *
 * Every grant is stored on this device only. Other applications on the same
 * device may request a grant by sending the exported
 * [ACTION_REQUEST_ACCESS] intent; the application then asks the user and
 * replies with the outcome. Grants can be revoked from the settings screen at
 * any time, which makes a previously authorized app lose its capability.
 *
 * Copyright (C) 2026 万古沧海
 */

/** Permission level granted to an application. */
enum class AuthLevel { READ, FULL }

/** One persisted authorization grant. */
data class AuthRecord(
    val packageName: String,
    val level: AuthLevel,
    val grantedAt: Long,
)

/**
 * Owns the authorization store and answers capability checks.
 */
class AuthorizationManager(context: Context) {

    private val prefs = AppPrefs(context)

    companion object {
        /** Intent action other applications use to request access. */
        const val ACTION_REQUEST_ACCESS = "com.wangucanghai.isposed.action.REQUEST_ACCESS"

        /** Intent action used to query a single package. */
        const val ACTION_QUERY_ACCESS = "com.wangucanghai.isposed.action.QUERY_ACCESS"

        const val EXTRA_REQUESTER = "requester"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_GRANTED = "granted"
        const val EXTRA_LEVEL_NAME = "level_name"

        const val KEY_REQUEST_PENDING = "com.wangucanghai.isposed.pending_request"
        const val KEY_REQUEST_PENDING_ACTION = "$KEY_REQUEST_PENDING#action"
    }

    /** All grants, most recent first. */
    fun records(): List<AuthRecord> =
        prefs.allAuthGrants().mapNotNull { entry ->
            GrantValue.decode(entry.value)?.let { AuthRecord(entry.key, it.level, it.grantedAt) }
        }.sortedByDescending { it.grantedAt }

    fun isAuthorized(packageName: String): Boolean =
        packageName.isNotBlank() && prefs.getAuthGrant(packageName) != null

    fun levelOf(packageName: String): AuthLevel? =
        GrantValue.decode(prefs.getAuthGrant(packageName) ?: return null)?.level

    fun grant(packageName: String, level: AuthLevel) {
        if (packageName.isBlank()) return
        prefs.setAuthGrant(packageName, GrantValue(level, System.currentTimeMillis()).encode())
    }

    fun revoke(packageName: String) = prefs.removeAuthGrant(packageName)

    fun revokeAll() = prefs.allAuthGrants().keys.toList().forEach { prefs.removeAuthGrant(it) }

    fun grantedCount(): Int = prefs.allAuthGrants().size
}

/** Raw persisted value for a grant, `LEVEL|timestamp`. */
data class GrantValue(val level: AuthLevel, val grantedAt: Long) {
    fun encode(): String = "$level|$grantedAt"

    companion object {
        fun decode(value: String): GrantValue? {
            val parts = value.split("|")
            val level = runCatching { AuthLevel.valueOf(parts.first()) }.getOrNull() ?: return null
            val time = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            return GrantValue(level, time)
        }
    }
}

/**
 * Builds the request intent used by other applications.
 */
object AccessIntent {

    fun request(level: AuthLevel = AuthLevel.READ): Intent =
        Intent(AuthorizationManager.ACTION_REQUEST_ACCESS)
            .putExtra(AuthorizationManager.EXTRA_LEVEL, level.name)

    fun query(packageName: String): Intent =
        Intent(AuthorizationManager.ACTION_QUERY_ACCESS)
            .putExtra(AuthorizationManager.EXTRA_REQUESTER, packageName)
}
