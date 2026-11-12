package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.ui.adapter.AppCheckAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Module scope editor.
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleScopeActivity : AppCompatActivity() {

    private val prefs by lazy { AppPrefs(this) }
    private val adapter = AppCheckAdapter()
    private var pkg: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scope)

        pkg = intent.getStringExtra("pkg") ?: return finish()
        val label = intent.getStringExtra("label") ?: pkg
        setTitle("作用域 · $label")

        findViewById<RecyclerView>(R.id.recycler_apps).apply {
            layoutManager = LinearLayoutManager(this@ModuleScopeActivity)
            adapter = this@ModuleScopeActivity.adapter
        }
        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            val scope = adapter.selected()
            CoroutineScope(Dispatchers.Main).launch {
                ModuleRepository(this@ModuleScopeActivity).setScope(pkg, scope)
                Toast.makeText(this@ModuleScopeActivity, "作用域已保存", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        load()
    }

    private fun load() {
        CoroutineScope(Dispatchers.Main).launch {
            val repo = ModuleRepository(this@ModuleScopeActivity)
            val apps = withContext(Dispatchers.IO) { repo.listApps() }
            val scope = withContext(Dispatchers.IO) { prefs.getScope(pkg) }
            adapter.submit(apps, scope)
        }
    }
}
