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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.update.UpdateChecker
import com.wangucanghai.isposed.update.UpdateInstaller
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Update screen: checks the latest release and updates in-app by downloading
 * the APK and handing it to the system package installer.
 *
 * Copyright (C) 2026 万古沧海
 */
class UpdateActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = stringResource(R.string.update_title), onBack = { finish() }) {
                    UpdateContent()
                }
            }
        }
    }
}

private fun versionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.8"
} catch (_: Exception) { "1.0.8" }

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
        Text(stringResource(R.string.update_current), style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        Text("v$currentVersion", style = MiuixTheme.textStyles.headline2)
    }
    SpacerH(16)
    SectionCard {
        Text(stringResource(R.string.update_latest), style = MiuixTheme.textStyles.title3)
        SpacerH(6)
        when {
            checking -> Text(stringResource(R.string.update_checking), style = MiuixTheme.textStyles.body2)
            info == null -> Text(stringResource(R.string.update_check_failed), style = MiuixTheme.textStyles.body2)
            info!!.hasUpdate -> {
                Text(
                    stringResource(R.string.update_found, info!!.latestVersion),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.primary,
                )
                SpacerH(8)
                Text(
                    info!!.notes.ifBlank { stringResource(R.string.update_no_notes) },
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
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
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.update_download_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@launch
                            }
                            val ok = withContext(Dispatchers.Main) { installer.install(apk) }
                            downloading = false
                            if (!ok) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.update_install_unavailable),
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                // The announcement is shown on the next launch, right
                                // before the agreement onboarding flow.
                                prefs.pendingChangelogVersion = info!!.latestVersion
                                prefs.pendingChangelogNotes = info!!.notes
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.update_install_started),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    enabled = !downloading,
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (downloading) R.string.update_downloading else R.string.update_download_install,
                        ),
                    )
                }
                if (downloading) {
                    SpacerH(10)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            else -> Text(
                stringResource(
                    R.string.update_up_to_date,
                    currentVersion,
                    info!!.latestVersion,
                ),
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
    SpacerH(28)
}
