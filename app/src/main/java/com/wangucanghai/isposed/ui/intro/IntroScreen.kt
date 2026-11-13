package com.wangucanghai.isposed.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.update.UpdateChecker
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * First-run introduction.
 *
 * Two modes share one surface. The first launch shows the product overview so
 * a new user understands what the application does. A later launch that
 * follows a version upgrade downloads the latest release information from the
 * server and shows the progress instead.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun IntroScreen(
    prefs: AppPrefs,
    onFinished: () -> Unit,
    force: Boolean = false,
) {
    val context = LocalContext.current
    val versionCode = remember { currentVersionCode(context) }
    val mode = remember {
        when {
            force -> IntroMode.WELCOME
            !prefs.introSeen -> IntroMode.WELCOME
            prefs.lastLaunchVersionCode != versionCode -> IntroMode.LOADING
            else -> IntroMode.SKIP
        }
    }
    when (mode) {
        IntroMode.WELCOME -> IntroWelcome(onStart = {
            prefs.introSeen = true
            prefs.lastLaunchVersionCode = versionCode
            onFinished()
        })

        IntroMode.LOADING -> IntroLoading(context = context, prefs = prefs, versionCode = versionCode, onFinished = onFinished)

        IntroMode.SKIP -> LaunchedEffect(Unit) { onFinished() }
    }
}

private enum class IntroMode { WELCOME, LOADING, SKIP }

/** Fetches the newest release information and finishes when it is available. */
@Composable
private fun IntroLoading(
    context: android.content.Context,
    prefs: AppPrefs,
    versionCode: Int,
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val result = try {
                UpdateChecker(context).check()
            } catch (t: Throwable) {
                Logger.w("intro release fetch failed: ${t.message}")
                null
            }
            if (result?.hasUpdate == true) {
                prefs.pendingChangelogVersion = result.latestVersion
                prefs.pendingChangelogNotes = result.notes
            }
        }
        prefs.introSeen = true
        prefs.lastLaunchVersionCode = versionCode
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
        SpacerH(18)
        Text(
            text = stringResource(R.string.intro_loading_title),
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Medium,
        )
        SpacerH(8)
        Text(
            text = stringResource(R.string.intro_loading_desc),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}

@Composable
private fun IntroWelcome(onStart: () -> Unit) {
    val features = remember {
        listOf(
            IntroFeature(
                icon = Icons.Filled.RocketLaunch,
                titleRes = R.string.intro_feature_1_title,
                descRes = R.string.intro_feature_1_desc,
            ),
            IntroFeature(
                icon = Icons.Filled.Terminal,
                titleRes = R.string.intro_feature_2_title,
                descRes = R.string.intro_feature_2_desc,
            ),
            IntroFeature(
                icon = Icons.Filled.AutoAwesome,
                titleRes = R.string.intro_feature_3_title,
                descRes = R.string.intro_feature_3_desc,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(44.dp))
        PulsingLogo()
        SpacerH(20)
        Text(
            text = stringResource(R.string.intro_title),
            style = MiuixTheme.textStyles.headline2,
            fontWeight = FontWeight.Bold,
        )
        SpacerH(6)
        Text(
            text = stringResource(R.string.intro_tagline),
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.primary,
        )
        SpacerH(10)
        Text(
            text = stringResource(R.string.intro_desc),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(26)

        features.forEach { feature ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                insideMargin = androidx.compose.foundation.layout.PaddingValues(18.dp),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MiuixTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(9.dp).size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(feature.titleRes),
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(feature.descRes),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }
            }
            SpacerH(12)
        }

        SpacerH(10)
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 14.dp,
            minHeight = 50.dp,
        ) {
            Text(stringResource(R.string.intro_cta))
        }
        Spacer(Modifier.height(30.dp))
    }
}

private data class IntroFeature(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
)

@Composable
private fun PulsingLogo() {
    val transition = rememberInfiniteTransition(label = "introPulse")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "introPulseScale",
    )
    Box(
        modifier = Modifier
            .size(92.dp)
            .background(MiuixTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.RocketLaunch,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(52.dp)
                .scale(scale),
        )
    }
}

private fun currentVersionCode(context: android.content.Context): Int = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) info.longVersionCode.toInt()
    else info.versionCode
} catch (_: Throwable) {
    0
}
