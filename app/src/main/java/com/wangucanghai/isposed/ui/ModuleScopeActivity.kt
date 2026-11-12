package com.wangucanghai.isposed.ui

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Module scope editor.
 *
 * Copyright (C) 2026 万古沧海
 */
class ModuleScopeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra("pkg") ?: run { finish(); return }
        val label = intent.getStringExtra("label") ?: pkg
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "作用域 · $label", onBack = { finish() }) {
                    ModuleScopeContent(pkg)
                }
            }
        }
    }
}

@Composable
private fun ModuleScopeContent(pkg: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    var apps by remember { mutableStateOf<List<ModuleRepository.AppEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val repo = ModuleRepository(context)
        val list = withContext(Dispatchers.IO) { repo.listApps() }
        val initial = withContext(Dispatchers.IO) { prefs.getScope(pkg) }
        apps = list
        selected = initial
        loaded = true
    }

    if (!loaded) {
        Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(apps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = app.packageName in selected,
                    onCheckedChange = { checked ->
                        selected = if (checked) selected + app.packageName else selected - app.packageName
                    },
                )
                Text(
                    text = "${app.label}  (${app.packageName})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SpacerH(16)
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val repo = ModuleRepository(context)
                        withContext(Dispatchers.IO) { repo.setScope(pkg, selected) }
                        Toast.makeText(context, "作用域已保存", Toast.LENGTH_SHORT).show()
                        (context as? ComponentActivity)?.finish()
                    }
                },
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Text("保存")
            }
            SpacerH(28)
        }
    }
}
