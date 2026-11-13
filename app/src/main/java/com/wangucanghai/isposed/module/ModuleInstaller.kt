package com.wangucanghai.isposed.module

import android.content.Context
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.adb.ShellExecutor
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.Constants
import com.wangucanghai.isposed.util.Logger
import java.io.File
import java.util.zip.ZipFile

/**
 * Installs AxManager-compatible plugin (non-root module) ZIPs.
 *
 * A module is a ZIP containing `module.prop` (metadata) and optionally
 * `customize.sh` (install script). We extract to $AXERONDIR/plugins/<id>,
 * run the install script, and require a Re-ignite (hot reboot) to take effect.
 *
 * Derived from AxManager plugin installer (Apache-2.0).
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleInstaller(
    private val context: Context,
    private val shell: ShellExecutor,
    private val prefs: AppPrefs,
) {

    data class ModuleProp(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Long,
        val author: String,
        val description: String,
    )

    data class InstallResult(
        val ok: Boolean,
        val message: String,
        val prop: ModuleProp?,
        val autoConverted: Boolean = false,
        val entryPoint: String? = null,
    )

    /** Parse module.prop from a module ZIP. */
    fun parseModuleProp(zipPath: String): ModuleProp? {
        return try {
        val zip = ZipFile(zipPath)
        val entry = zip.getEntry("module.prop") ?: zip.getEntry("META-INF/module.prop")
        if (entry == null) {
            zip.close()
            return null
        }
        val text = zip.getInputStream(entry).bufferedReader().readText()
        zip.close()
        val map = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .associate { kv ->
                val i = kv.indexOf('=')
                kv.substring(0, i).trim() to kv.substring(i + 1).trim()
            }
        val id = map["id"] ?: return null
        if (!Regex("^[a-zA-Z][a-zA-Z0-9._-]+$").matches(id)) {
            Logger.e("Module id '$id' invalid")
            return null
        }
        ModuleProp(
            id = id,
            name = map["name"] ?: id,
            version = map["version"] ?: "1.0",
            versionCode = map["versionCode"]?.toLongOrNull() ?: 1,
            author = map["author"] ?: "unknown",
            description = map["description"] ?: "",
        )
    } catch (t: Throwable) {
        Logger.e("parseModuleProp: ${t.message}")
        null
    }
    }

    /** Detect the Xposed/LSPosed entry point from a module ZIP, if any. */
    fun detectXposedEntry(zipPath: String): String? {
        return try {
            val zip = ZipFile(zipPath)
            val entry = zip.getEntry("assets/xposed_init") ?: zip.getEntry("xposed_init")
            if (entry == null) {
                zip.close()
                return null
            }
            val text = zip.getInputStream(entry).bufferedReader().readText()
            zip.close()
            text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
        } catch (t: Throwable) {
            Logger.e("detectXposedEntry: ${t.message}")
            null
        }
    }

    /**
     * Install a module ZIP. Returns result; caller should offer Re-ignite.
     */
    fun install(zipPath: String): InstallResult {
        val prop = parseModuleProp(zipPath)
            ?: return InstallResult(false, context.getString(R.string.modules_prop_invalid), null)
        val pluginDir = Constants.AXERON_PLUGIN_DIR + "/" + prop.id
        val dest = File(context.cacheDir, "module_extract_" + prop.id)
        if (dest.exists()) dest.deleteRecursively()
        dest.mkdirs()

        // extract
        return try {
            ZipFile(zipPath).use { zip ->
                zip.entries().asSequence().forEach { e ->
                    val target = File(dest, e.name)
                    if (!target.canonicalPath.startsWith(dest.canonicalPath + File.separator) &&
                        target.canonicalPath != dest.canonicalPath
                    ) return@forEach
                    if (e.isDirectory) target.mkdirs()
                    else {
                        target.parentFile?.mkdirs()
                        zip.getInputStream(e).use { input ->
                            target.outputStream().use { input.copyTo(it) }
                        }
                    }
                }
            }
            // push whole module dir via adb shell (cp from tmp) — simplest reliable path
            val tmp = "/data/local/tmp/module_" + prop.id
            shell.exec("rm -rf $tmp; mkdir -p $tmp")
            pushDir(dest, tmp)
            shell.exec("rm -rf $pluginDir; cp -r $tmp $pluginDir; rm -rf $tmp")
            // run customize.sh if present
            val customize = shell.exec("test -f $pluginDir/customize.sh && sh $pluginDir/customize.sh || echo NONE")
            Logger.i("customize.sh -> ${customize.output.take(200)}")
            // Merge convert-into-install: auto-detect an Xposed entry and register the
            // adapter so the module is usable under the current (Magisk/LSPosed) runtime
            // without a separate manual convert step.
            val entry = detectXposedEntry(zipPath)
            if (entry != null) {
                val adapterJson = buildString {
                    appendLine("{")
                    appendLine("  \"sourceModule\": \"${prop.id}\",")
                    appendLine("  \"name\": \"${prop.name}\",")
                    appendLine("  \"entryPoints\": [\"$entry\"]")
                    append("}")
                }
                prefs.setAdapter(prop.id, adapterJson)
                Logger.i("auto-converted ${prop.id} entry=$entry")
                InstallResult(
                    true,
                    context.getString(R.string.modules_installed_converted, prop.name, prop.version),
                    prop,
                    autoConverted = true,
                    entryPoint = entry,
                )
            } else {
                InstallResult(
                    true,
                    context.getString(R.string.modules_installed, prop.name, prop.version),
                    prop,
                )
            }
        } catch (t: Throwable) {
            Logger.e("Install failed: ${t.message}")
            InstallResult(false, context.getString(R.string.modules_install_failed_detail, t.message ?: ""), prop)
        } finally {
            dest.deleteRecursively()
        }
    }

    private fun pushDir(dir: File, remoteBase: String) {
        dir.walkTopDown().filter { it.isFile }.forEach { f ->
            val rel = f.relativeTo(dir).path
            val remote = "$remoteBase/$rel"
            shell.exec("mkdir -p ${remote.substringBeforeLast('/')}")
            pushFile(f, remote)
        }
    }

    private fun pushFile(f: File, remote: String) {
        try {
            val key = com.wangucanghai.isposed.adb.AdbKeyStore(context).key()
            com.wangucanghai.isposed.adb.AdbClient(prefs.adbHost, prefs.adbPort, key).use { c ->
                c.connect()
                c.push(f.readBytes(), remote)
            }
        } catch (t: Throwable) {
            Logger.e("pushFile failed: ${t.message}")
        }
    }

    /** Remove a module and run its uninstall.sh. */
    fun uninstall(moduleId: String): ShellExecutor.ShellResult {
        val dir = Constants.AXERON_PLUGIN_DIR + "/" + moduleId
        return shell.exec("test -f $dir/uninstall.sh && sh $dir/uninstall.sh; rm -rf $dir")
    }

    /** Re-ignite: hot reboot so modules take effect (AxManager Re-ignite). */
    fun reIgnite(): ShellExecutor.ShellResult = shell.exec(
        "am broadcast -a android.intent.action.REBOOT; " +
            "cmd activity restart; reboot 2>/dev/null || true"
    )
}
