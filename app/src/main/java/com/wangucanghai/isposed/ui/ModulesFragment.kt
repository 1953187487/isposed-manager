package com.wangucanghai.isposed.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleInfo
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.module.ModuleInstaller
import com.wangucanghai.isposed.adb.ShellExecutor
import com.wangucanghai.isposed.ui.adapter.ModuleAdapter
import com.wangucanghai.isposed.ui.notice.Notices
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modules page: lists Xposed/LSPosed modules, enables/disables, edits scope,
 * installs root-free modules, and hosts the AI direct-development entry point.
 *
 * Copyright (C) 2026 万古沧海
 */
class ModulesFragment : Fragment() {

    private lateinit var repository: ModuleRepository
    private lateinit var adapter: ModuleAdapter
    private val prefs by lazy { AppPrefs(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_modules, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = ModuleRepository(requireContext())
        adapter = ModuleAdapter()
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_modules)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh).setOnRefreshListener { load() }

        adapter.onToggle = { module, enabled ->
            CoroutineScope(Dispatchers.Main).launch {
                repository.setEnabled(module.packageName, enabled)
                load()
            }
        }
        adapter.onClick = { module ->
            AlertDialog.Builder(requireContext())
                .setTitle(module.label)
                .setMessage(
                    "包名：${module.packageName}\n版本：${module.versionName}\n" +
                        "描述：${module.description ?: "无"}\n作用域：${module.scope.size} 个"
                )
                .setPositiveButton("编辑作用域") { _, _ ->
                    startActivity(
                        Intent(requireContext(), ModuleScopeActivity::class.java)
                            .putExtra("pkg", module.packageName)
                            .putExtra("label", module.label)
                    )
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // AI direct development — AI notice first
        view.findViewById<View>(R.id.btn_ai_dev).setOnClickListener {
            Notices.showAiNotice(requireActivity()) {
                startActivity(Intent(requireContext(), DevActivity::class.java))
            }
        }
        view.findViewById<View>(R.id.btn_ai_convert).setOnClickListener {
            Notices.showAiNotice(requireActivity()) {
                startActivity(Intent(requireContext(), ConvertActivity::class.java))
            }
        }
        view.findViewById<MaterialButton>(R.id.btn_install_module).setOnClickListener { installModule() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        CoroutineScope(Dispatchers.Main).launch {
            val modules = withContext(Dispatchers.IO) { repository.loadModules() }
            adapter.submit(modules)
            view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)?.isRefreshing = false
        }
    }

    private fun installModule() {
        val launcher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            CoroutineScope(Dispatchers.Main).launch {
                val zipPath = copyToCache(uri)
                val installer = ModuleInstaller(requireContext(), ShellExecutor(requireContext(), prefs), prefs)
                val prop = withContext(Dispatchers.IO) { installer.parseModuleProp(zipPath) }
                if (prop == null) {
                    Toast.makeText(requireContext(), "无效的模块包（缺少 module.prop）", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val result = withContext(Dispatchers.IO) { installer.install(zipPath) }
                AlertDialog.Builder(requireContext())
                    .setTitle("安装结果")
                    .setMessage(
                        "模块：${prop.name} v${prop.version}\n作者：${prop.author}\n\n" +
                            result.message
                    )
                    .setPositiveButton("热重启生效 (Re-ignite)") { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) { installer.reIgnite() }
                        }
                    }
                    .setNegativeButton("稍后", null)
                    .show()
            }
        }
        launcher.launch("application/zip")
    }

    private fun copyToCache(uri: Uri): String {
        val dest = java.io.File(requireContext().cacheDir, "module_${System.currentTimeMillis()}.zip")
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        Logger.i("Module zip cached: ${dest.absolutePath}")
        return dest.absolutePath
    }
}
