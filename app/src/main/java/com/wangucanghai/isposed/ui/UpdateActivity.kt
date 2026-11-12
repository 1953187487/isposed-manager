package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.update.UpdateChecker
import com.wangucanghai.isposed.update.UpdateInstaller
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Update screen: checks the latest release and updates in-app by downloading
 * the APK and handing it to the system package installer.
 *
 * Copyright (C) 2026 万古沧海
 */
class UpdateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = "检查更新", onBack = { finish() }) {
                    UpdateContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaffoldPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            content()
        }
    }
}

private fun versionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.2"
} catch (_: Exception) { "1.0.2" }

@Composable
private fun UpdateContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    var checking by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        checking = true
        val r = withContext(Dispatchers.IO) { UpdateChecker(context).check() }
        prefs.lastCheckedVersion = r.latestVersion
        prefs.lastUpdateNotes = r.notes
        info = r
        checking = false
        Logger.i("Update check: ${r.latestVersion} hasUpdate=${r.hasUpdate}")
    }

    val currentVersion = versionName(context)
    SectionCard {
        Text("当前版本", style = MaterialTheme.typography.titleSmall)
        SpacerH(6)
        Text("v$currentVersion", style = MaterialTheme.typography.headlineSmall)
    }
    SpacerH(16)
    SectionCard {
        Text("最新版本", style = MaterialTheme.typography.titleSmall)
        SpacerH(6)
        when {
            checking -> Text("检查中…", style = MaterialTheme.typography.bodyMedium)
            info == null -> Text("检查失败（网络或限流）", style = MaterialTheme.typography.bodyMedium)
            info!!.hasUpdate -> {
                Text(
                    "发现新版本 v${info!!.latestVersion}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                SpacerH(8)
                Text(
                    info!!.notes.ifBlank { "无更新说明" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
                SpacerH(12)
                Button(
                    onClick = {
                        downloading = true
                        scope.launch {
                            val installer = UpdateInstaller(context)
                            val apk = withContext(Dispatchers.IO) { installer.download(info!!.downloadUrl) }
                            if (apk == null) {
                                downloading = false
                                Toast.makeText(context, "下载失败，请检查网络", Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            val ok = withContext(Dispatchers.Main) { installer.install(apk) }
                            downloading = false
                            if (!ok) {
                                Toast.makeText(context, "无法调起安装，请开启「允许安装未知应用」", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !downloading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (downloading) "下载中…" else "下载并安装")
                }
                if (downloading) {
                    SpacerH(10)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            else -> Text(
                "已是最新版本（当前 v$currentVersion / 最新 ${info!!.latestVersion}）",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    SpacerH(28)
}
