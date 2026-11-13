package com.wangucanghai.isposed.ui.authorization

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AuthLevel
import com.wangucanghai.isposed.core.AuthorizationManager
import com.wangucanghai.isposed.ui.BaseActivity
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Per-application authorization management.
 *
 * Lists every installed user application and lets the owner grant or revoke the
 * Shell capability. A grant stays on this device only and can be revoked from
 * this screen at any time, which immediately removes the capability from the
 * requesting app.
 *
 * Copyright (C) 2026 万古沧海
 */
class AuthorizationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = stringResource(R.string.auth_title), onBack = { finish() }) {
                    AuthorizationScreen()
                }
            }
        }
    }
}

private data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
)

@Composable
private fun AuthorizationScreen() {
    val context = LocalContext.current
    val manager = remember { AuthorizationManager(context) }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    val grants = remember(tick) { manager.records().associateBy { it.packageName } }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpacerH(4)
        Text(
            text = stringResource(R.string.auth_desc),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(10)

        SectionCard {
            Text(
                text = stringResource(R.string.auth_summary, grants.size),
                style = MiuixTheme.textStyles.title3,
            )
            SpacerH(8)
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.auth_search_hint),
                useLabelAsPlaceholder = true,
                singleLine = true,
                cornerRadius = 14.dp,
            )
            SpacerH(8)
            if (grants.isNotEmpty()) {
                TextButton(
                    text = stringResource(R.string.auth_revoke_all),
                    onClick = {
                        manager.revokeAll()
                        tick++
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SpacerH(10)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.packageName }) { entry ->
                val grant = grants[entry.packageName]
                AppAuthRow(
                    entry = entry,
                    granted = grant != null,
                    level = grant?.level,
                    onToggle = {
                        if (grant == null) manager.grant(entry.packageName, AuthLevel.READ)
                        else manager.revoke(entry.packageName)
                        tick++
                    },
                    onLevel = { level ->
                        manager.grant(entry.packageName, level)
                        tick++
                    },
                )
                SpacerH(6)
            }
        }
    }
}

@Composable
private fun AppAuthRow(
    entry: AppEntry,
    granted: Boolean,
    level: AuthLevel?,
    onToggle: () -> Unit,
    onLevel: (AuthLevel) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MiuixTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MiuixTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (entry.icon != null) {
                    Image(
                        bitmap = entry.icon.asImageBitmap(),
                        contentDescription = entry.label,
                        modifier = Modifier.padding(6.dp).size(28.dp),
                    )
                }
            }
            SpacerH(10)
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MiuixTheme.textStyles.body2)
                Text(
                    text = entry.packageName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
            if (granted) {
                Text(
                    text = if (level == AuthLevel.FULL) stringResource(R.string.auth_level_full)
                    else stringResource(R.string.auth_level_read),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
                SpacerH(6)
                TextButton(
                    text = if (level == AuthLevel.FULL) stringResource(R.string.auth_level_read)
                    else stringResource(R.string.auth_level_full),
                    onClick = { onLevel(if (level == AuthLevel.FULL) AuthLevel.READ else AuthLevel.FULL) },
                    cornerRadius = 12.dp,
                )
            }
            TextButton(
                text = if (granted) stringResource(R.string.auth_revoke)
                else stringResource(R.string.auth_grant),
                onClick = onToggle,
                cornerRadius = 12.dp,
            )
        }
    }
}

private fun loadApps(context: android.content.Context): List<AppEntry> {
    val pm = context.packageManager
    val self = context.packageName
    return try {
        pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != self }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { info ->
                val label = runCatching { pm.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName)
                AppEntry(
                    packageName = info.packageName,
                    label = label,
                    icon = drawableToBitmap(runCatching { info.loadIcon(pm) }.getOrNull()),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    } catch (t: Throwable) {
        emptyList()
    }
}

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable) {
        return try {
            drawable.bitmap
        } catch (_: Throwable) {
            null
        }
    }
    val intrinsic = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    return try {
        val bitmap = Bitmap.createBitmap(intrinsic, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Throwable) {
        null
    }
}
