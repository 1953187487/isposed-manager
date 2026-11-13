package com.wangucanghai.isposed.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Shared preferences store. Keeps AI config, ADB config, module scopes and update metadata.
 *
 * Copyright (C) 2026 万古沧海
 */
class AppPrefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("isposed_manager_prefs", Context.MODE_PRIVATE)

    // ---------- AI ----------
    var aiBaseUrl: String
        get() = sp.getString("ai_base_url", "") ?: ""
        set(v) = sp.edit().putString("ai_base_url", v.trimEnd('/')).apply()

    var aiApiKey: String
        get() = sp.getString("ai_api_key", "") ?: ""
        set(v) = sp.edit().putString("ai_api_key", v).apply()

    var aiModel: String
        get() = sp.getString("ai_model", "") ?: ""
        set(v) = sp.edit().putString("ai_model", v).apply()

    var aiTemperature: Float
        get() = sp.getFloat("ai_temperature", 0.7f)
        set(v) = sp.edit().putFloat("ai_temperature", v).apply()

    // ---------- ADB / wireless debugging ----------
    var adbHost: String
        get() = sp.getString("adb_host", "127.0.0.1") ?: "127.0.0.1"
        set(v) = sp.edit().putString("adb_host", v).apply()

    var adbPort: Int
        get() = sp.getInt("adb_port", Constants.ADB_DEFAULT_PORT)
        set(v) = sp.edit().putInt("adb_port", v).apply()

    var adbPaired: Boolean
        get() = sp.getBoolean("adb_paired", false)
        set(v) = sp.edit().putBoolean("adb_paired", v).apply()

    // ---------- module scope (self-managed fallback) ----------
    fun getScope(pkg: String): Set<String> =
        sp.getStringSet(Constants.PREF_SCOPE + ":" + pkg, emptySet()) ?: emptySet()

    fun setScope(pkg: String, scope: Set<String>) =
        sp.edit().putStringSet(Constants.PREF_SCOPE + ":" + pkg, scope).apply()

    // ---------- agreement ----------
    var agreementAccepted: Boolean
        get() = sp.getBoolean("agreement_accepted", false)
        set(v) = sp.edit().putBoolean("agreement_accepted", v).apply()

    var agreementVersion: Int
        get() = sp.getInt("agreement_version", 0)
        set(v) = sp.edit().putInt("agreement_version", v).apply()

    /** AI 须知是否已同意（单独记录，AI 功能入口强制提醒）。 */
    var aiNoticeAccepted: Boolean
        get() = sp.getBoolean("ai_notice_accepted", false)
        set(v) = sp.edit().putBoolean("ai_notice_accepted", v).apply()

    // ---------- language ----------
    /** 界面语言：system / zh / en。 */
    var appLanguage: String
        get() = sp.getString("app_language", "system") ?: "system"
        set(v) = sp.edit().putString("app_language", v).apply()

    // ---------- update ----------
    var lastCheckedVersion: String
        get() = sp.getString("last_checked_version", "") ?: ""
        set(v) = sp.edit().putString("last_checked_version", v).apply()

    var lastUpdateNotes: String
        get() = sp.getString("last_update_notes", "") ?: ""
        set(v) = sp.edit().putString("last_update_notes", v).apply()

    /** 已安装但尚未展示的更新公告版本号，空串表示无待展示公告。 */
    var pendingChangelogVersion: String
        get() = sp.getString("pending_changelog_version", "") ?: ""
        set(v) = sp.edit().putString("pending_changelog_version", v).apply()

    /** 待展示的更新公告正文。 */
    var pendingChangelogNotes: String
        get() = sp.getString("pending_changelog_notes", "") ?: ""
        set(v) = sp.edit().putString("pending_changelog_notes", v).apply()

    // ---------- per-app authorization grants ----------
    fun getAuthGrant(packageName: String): String? =
        sp.getString("auth_grant:" + packageName, null)

    fun setAuthGrant(packageName: String, encoded: String) =
        sp.edit().putString("auth_grant:" + packageName, encoded).apply()

    fun removeAuthGrant(packageName: String) =
        sp.edit().remove("auth_grant:" + packageName).apply()

    fun allAuthGrants(): Map<String, String> =
        sp.all.entries
            .filter { it.key.startsWith("auth_grant:") }
            .associate { it.key.removePrefix("auth_grant:") to (it.value as String) }

    // ---------- introduction / first-run ----------
    /** 是否已看过产品介绍页。 */
    var introSeen: Boolean
        get() = sp.getBoolean("intro_seen", false)
        set(v) = sp.edit().putBoolean("intro_seen", v).apply()

    /** 上次启动时的 versionCode，用于检测「新版本安装后首次启动」。 */
    var lastLaunchVersionCode: Int
        get() = sp.getInt("last_launch_version_code", 0)
        set(v) = sp.edit().putInt("last_launch_version_code", v).apply()

    // ---------- pending external access request ----------
    var pendingRequester: String
        get() = sp.getString("pending_requester", "") ?: ""

    set(v) = sp.edit().putString("pending_requester", v).apply()

    var pendingRequestTitle: String
        get() = sp.getString("pending_request_title", "") ?: ""

    set(v) = sp.edit().putString("pending_request_title", v).apply()

    var pendingRequestLevel: String
        get() = sp.getString("pending_request_level", "READ") ?: "READ"

    set(v) = sp.edit().putString("pending_request_level", v).apply()

    fun clearPendingRequest() = sp.edit()
        .putString("pending_requester", "")
        .putString("pending_request_title", "")
        .putString("pending_request_level", "READ")
        .apply()

    // ---------- auto-converted adapters (install-time module conversion) ----------
    /** Adapter descriptor JSON for a module id that was auto-converted on install. */
    fun getAdapter(moduleId: String): String? =
        sp.getString("adapter:" + moduleId, null)

    fun setAdapter(moduleId: String, json: String) =
        sp.edit().putString("adapter:" + moduleId, json).apply()

    fun allAdapters(): Map<String, String> =
        sp.all.entries
            .filter { it.key.startsWith("adapter:") }
            .associate { it.key.removePrefix("adapter:") to (it.value as String) }
}
