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
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.wangucanghai.isposed.R
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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    val queryState = remember { TextFieldState() }
    val query = queryState.text

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
                val installer = ModuleInstaller(context, ShellExecutor(context, prefs), prefs)
                val prop = withContext(Dispatchers.IO) { installer.parseModuleProp(zipPath) }
                if (prop == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.modules_invalid),
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }
                val result = withContext(Dispatchers.IO) { installer.install(zipPath) }
                android.app.AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.modules_install_result))
                    .setMessage(
                        context.getString(R.string.modules_install_detail, prop.name, prop.version, prop.author, result.message),
                    )
                    .setPositiveButton(context.getString(R.string.ok)) { _, _ -> }
                    .show()
                Logger.i("Module install: ${result.message}")
            } catch (t: Throwable) {
                Toast.makeText(
                    context,
                    context.getString(R.string.modules_install_failed, t.message ?: ""),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { picker.launch("application/zip") },
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    SpacerH(4)
                    Text(stringResource(R.string.modules_install), style = MiuixTheme.textStyles.button)
                }
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.modules_title),
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                TextField(
                    state = queryState,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.modules_search),
                    useLabelAsPlaceholder = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    cornerRadius = 16.dp,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipPill(
                        icon = Icons.Filled.AutoAwesome,
                        label = stringResource(R.string.modules_ai_convert),
                        onClick = { context.startActivity(Intent(context, ConvertActivity::class.java)) },
                    )
                    ChipPill(
                        icon = Icons.Outlined.Code,
                        label = stringResource(R.string.modules_ai_dev),
                        onClick = { context.startActivity(Intent(context, DevActivity::class.java)) },
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))

            val filtered = if (query.isBlank()) modules
            else modules.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (loading) stringResource(R.string.modules_loading)
                        else stringResource(R.string.modules_empty),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
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
                            modifier = Modifier.animateItem(),
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
    modifier: Modifier = Modifier,
    module: ModuleInfo,
    onToggle: (Boolean) -> Unit,
    onScope: () -> Unit,
    onConvert: () -> Unit,
) {
    Card(
        modifier = modifier.animateContentSize(),
        cornerRadius = 20.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawableIcon(module.icon, 44.dp)
                SpacerH(4)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = module.label,
                        style = MiuixTheme.textStyles.title2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${module.packageName}  ·  v${module.versionName}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
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
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onScope, cornerRadius = 12.dp, minHeight = 36.dp) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.modules_scope_count, module.scope.size))
                }
                Button(onClick = onConvert, cornerRadius = 12.dp, minHeight = 36.dp) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.modules_ai_convert))
                }
            }
        }
    }
}

@Composable
private fun ChipPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MiuixTheme.colorScheme.secondaryContainer,
        contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            SpacerH(4)
            Text(label, style = MiuixTheme.textStyles.footnote2)
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
