package com.wangucanghai.isposed.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.Constants
import com.wangucanghai.isposed.lsposed.LsposedBridge
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers installed Xposed modules (meta-data based, LSPosed-compatible) and
 * manages their enabled/scope state.
 *
 * This layer follows the LSPosed module contract: a module is an APK declaring
 * `xposedmodule=true` meta-data. Derived from LSPosed (GPL-3.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleRepository(private val context: Context) {

    private val prefs = AppPrefs(context)

    /**
     * List installed Xposed modules. When an LSPosed daemon is reachable we use its
     * authoritative state; otherwise we fall back to self-managed preferences.
     */
    suspend fun loadModules(): List<ModuleInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val lsposed = LsposedBridge.tryConnect(context)
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<ModuleInfo>()
        for (app in packages) {
            val meta = app.metaData ?: continue
            if (meta.getBoolean("xposedmodule", false)) {
                val enabled = lsposed?.isModuleEnabled(app.packageName)
                    ?: prefs.getScope(app.packageName).isNotEmpty()
                val scope = lsposed?.getModuleScope(app.packageName)
                    ?: prefs.getScope(app.packageName).toList()
                result += buildInfo(pm, app, meta, enabled, scope)
            }
        }
        // Sort by label
        result.sortedBy { it.label.lowercase() }
    }

    private fun buildInfo(
        pm: PackageManager,
        app: ApplicationInfo,
        meta: Bundle,
        enabled: Boolean,
        scope: List<String>,
    ): ModuleInfo {
        val label = pm.getApplicationLabel(app).toString()
        val version = try {
            val info = pm.getPackageInfo(app.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode to (info.versionName ?: "")
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong() to (info.versionName ?: "")
            }
        } catch (_: Exception) {
            0L to ""
        }
        return ModuleInfo(
            packageName = app.packageName,
            label = label,
            versionName = version.second,
            versionCode = version.first,
            icon = pm.getApplicationIcon(app),
            description = meta.getString("xposeddescription"),
            minVersion = meta.getInt("xposedminversion", 0),
            isEnabled = enabled,
            isActive = enabled && scope.isNotEmpty(),
            scope = scope,
        )
    }

    /** Toggle a module. Uses LSPosed daemon when available, else local prefs. */
    suspend fun setEnabled(pkg: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val lsposed = LsposedBridge.tryConnect(context)
        if (lsposed != null) {
            lsposed.setModuleEnabled(pkg, enabled)
        } else {
            val scope = prefs.getScope(pkg)
            if (enabled) {
                if (scope.isEmpty()) prefs.setScope(pkg, setOf(context.packageName))
            } else {
                prefs.setScope(pkg, emptySet())
            }
        }
        Logger.i("Module $pkg ${if (enabled) "enabled" else "disabled"}")
    }

    /** Set module scope (target packages). */
    suspend fun setScope(pkg: String, scope: Set<String>) = withContext(Dispatchers.IO) {
        val lsposed = LsposedBridge.tryConnect(context)
        if (lsposed != null) {
            lsposed.setModuleScope(pkg, scope.toList())
        } else {
            prefs.setScope(pkg, scope)
        }
        Logger.i("Module $pkg scope updated: ${scope.size} targets")
    }

    /** All installed user apps, for scope picking. */
    suspend fun listApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName == "android" }
            .map {
                AppEntry(
                    it.packageName,
                    pm.getApplicationLabel(it).toString(),
                    pm.getApplicationIcon(it),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    data class AppEntry(val packageName: String, val label: String, val icon: android.graphics.drawable.Drawable?)
}
