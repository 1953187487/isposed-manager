package com.wangucanghai.isposed.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.adb.WirelessDebugger
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.Constants
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.ui.notice.Notices
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home page: activation status, stats and quick actions.
 *
 * Copyright (C) 2026 万古沧海
 */
class HomeFragment : Fragment() {

    private val prefs by lazy { AppPrefs(requireContext()) }
    private val debugger by lazy { WirelessDebugger(requireContext(), prefs) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshStatus()

        view.findViewById<MaterialButton>(R.id.btn_activate).setOnClickListener {
            // AI 无关：激活直接执行
            activate()
        }
        view.findViewById<MaterialButton>(R.id.btn_pair).setOnClickListener { pair() }

        view.findViewById<View>(R.id.action_ai_dev).setOnClickListener {
            Notices.showAiNotice(requireActivity()) {
                startActivity(Intent(requireContext(), DevActivity::class.java))
            }
        }
        view.findViewById<View>(R.id.action_convert).setOnClickListener {
            Notices.showAiNotice(requireActivity()) {
                startActivity(Intent(requireContext(), ConvertActivity::class.java))
            }
        }
        view.findViewById<View>(R.id.action_shell).setOnClickListener {
            startActivity(Intent(requireContext(), ShellActivity::class.java))
        }
        view.findViewById<View>(R.id.action_update).setOnClickListener {
            startActivity(Intent(requireContext(), UpdateActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_agreement).setOnClickListener {
            Notices.showAgreement(requireActivity(), prefs)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val active = prefs.adbPaired
        val statusText = view?.findViewById<TextView>(R.id.status_text)
        val detail = view?.findViewById<TextView>(R.id.status_detail)
        statusText?.text = if (active) getString(R.string.home_status_active)
        else getString(R.string.home_status_inactive)
        detail?.text = if (active) "${prefs.adbHost}:${prefs.adbPort}"
        else getString(R.string.home_status_rootless)

        // stats
        CoroutineScope(Dispatchers.Main).launch {
            val modules = withContext(Dispatchers.IO) { ModuleRepository(requireContext()).loadModules() }
            view?.findViewById<TextView>(R.id.stat_modules)?.text = modules.size.toString()
            view?.findViewById<TextView>(R.id.stat_enabled)?.text =
                modules.count { it.isEnabled }.toString()
        }
    }

    private fun activate() {
        val host = prefs.adbHost
        val port = prefs.adbPort
        if (!prefs.adbPaired) {
            Toast.makeText(requireContext(), "请先完成无线配对", Toast.LENGTH_SHORT).show()
            pair()
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val ok = withContext(Dispatchers.IO) { debugger.activate(host, port) }
            Toast.makeText(requireContext(), if (ok) "激活成功" else "激活失败", Toast.LENGTH_SHORT).show()
            refreshStatus()
        }
    }

    private fun pair() {
        val inputs = arrayOf("主机 IP", "配对端口", "配对码(6位)")
        val defaults = arrayOf(prefs.adbHost, "", "")
        val views = arrayOf(
            android.widget.EditText(requireContext()),
            android.widget.EditText(requireContext()),
            android.widget.EditText(requireContext()),
        )
        views.forEachIndexed { i, v -> v.hint = inputs[i]; v.setText(defaults[i]) }
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            views.forEach {
                addView(it)
                addView(android.view.View(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, pad / 4
                    )
                })
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("无线调试配对")
            .setView(container)
            .setPositiveButton("配对") { _, _ ->
                val host = views[0].text.toString().ifBlank { "127.0.0.1" }
                val port = views[1].text.toString().toIntOrNull() ?: 37129
                val code = views[2].text.toString()
                CoroutineScope(Dispatchers.Main).launch {
                    val ok = withContext(Dispatchers.IO) { debugger.pair(host, port, code) }
                    Toast.makeText(requireContext(), if (ok) "配对成功，可激活" else "配对失败", Toast.LENGTH_LONG).show()
                    Logger.i("Pair result: $ok")
                    refreshStatus()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
