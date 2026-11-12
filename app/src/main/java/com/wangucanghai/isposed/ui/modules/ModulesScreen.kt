package com.wangucanghai.isposed.ui.modules

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.wangucanghai.isposed.adb.ShellExecutor
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.data.ModuleInfo
import com.wangucanghai.isposed.data.ModuleRepository
import com.wangucanghai.isposed.module.ModuleInstaller
import com.wangucanghai.isposed.ui.ConvertActivity
import com.wangucanghai.isposed.ui.DevActivity
import com.wangucanghai.isposed.ui.ModuleScopeActivity
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Module management: list installed Xposed modules, enable/scope, install from ZIP.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun ModulesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    val repo = remember { ModuleRepository(context) }

    var modules by remember { mutableStateOf<List<ModuleInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            loading = true
            modules = repo.loadModules()
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val zipPath = withContext(Dispatchers.IO) { copyToCache(context, uri) }
                val installer = ModuleInstaller(context, ShellExecutor(prefs), prefs)
                val prop = withContext(Dispatchers.IO) { installer.parseModuleProp(zipPath) }
                if (prop == null) {
                    Toast.makeText(context, "无效的模块包（缺少 module.prop）", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val result = withContext(Dispatchers.IO) { installer.install(zipPath) }
                android.app.AlertDialog.Builder(context)
                    .setTitle("安装结果")
                    .setMessage(
                        "模块：${prop.name} v${prop.version}\n作者：${prop.author}\n\n${result.message}"
                    )
                    .setPositiveButton("好") { _, _ -> }
                    .show()
                Logger.i("Module install: ${result.message}")
            } catch (t: Throwable) {
                Toast.makeText(context, "安装失败：${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picker.launch("application/zip") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("安装模块") },
                shape = RoundedCornerShape(18.dp),
            )
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
                Text(
                    text = "模块",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索模块") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = { context.startActivity(Intent(context, ConvertActivity::class.java)) },
                        label = { Text("AI 转换") },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    FilterChip(
                        selected = false,
                        onClick = { context.startActivity(Intent(context, DevActivity::class.java)) },
                        label = { Text("AI 开发") },
                        leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))

            val filtered = if (query.isBlank()) modules
            else modules.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (loading) "加载中…" else "未发现 Xposed 模块",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filtered, key = { it.packageName }) { mod ->
                        ModuleCard(
                            module = mod,
                            onToggle = { enabled ->
                                scope.launch {
                                    repo.setEnabled(mod.packageName, enabled)
                                    reload()
                                }
                            },
                            onScope = {
                                val i = Intent(context, ModuleScopeActivity::class.java)
                                i.putExtra("pkg", mod.packageName)
                                i.putExtra("label", mod.label)
                                context.startActivity(i)
                            },
                            onConvert = {
                                val i = Intent(context, ConvertActivity::class.java)
                                i.putExtra("pkg", mod.packageName)
                                context.startActivity(i)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: ModuleInfo,
    onToggle: (Boolean) -> Unit,
    onScope: () -> Unit,
    onConvert: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawableIcon(module.icon, 44.dp)
                SpacerH(4)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = module.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${module.packageName}  ·  v${module.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = module.isEnabled,
                    onCheckedChange = { onToggle(it) },
                )
            }
            if (!module.description.isNullOrBlank()) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onScope, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                    Text("作用域 (${module.scope.size})")
                }
                OutlinedButton(onClick = onConvert, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                    Text("AI 转换")
                }
            }
        }
    }
}

@Composable
fun DrawableIcon(drawable: Drawable?, size: Dp) {
    if (drawable != null) {
        val bmp = remember(drawable) { drawable.toBitmap(96, 96) }
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        androidx.compose.foundation.layout.Spacer(Modifier.size(size))
    }
}

private fun copyToCache(context: android.content.Context, uri: Uri): String {
    val file = java.io.File(context.cacheDir, "pick_${System.currentTimeMillis()}.zip")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { input.copyTo(it) }
    }
    return file.absolutePath
}
