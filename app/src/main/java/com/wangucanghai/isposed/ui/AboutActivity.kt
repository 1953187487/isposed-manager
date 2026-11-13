package com.wangucanghai.isposed.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * About page: application information, signature, rewritten module features,
 * author and license.
 *
 * Copyright (C) 2026 万古沧海
 */
class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = stringResource(R.string.about_title), onBack = { finish() }) {
                    AboutContent()
                }
            }
        }
    }
}

private val LSPOSED_BRAND = Color(0xFFF48FB1)

private data class RewrittenFeature(val icon: ImageVector, val title: String, val desc: String)

@Composable
private fun AboutContent() {
    val context = LocalContext.current
    val packageInfo = remember { packageInfo(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            PulsingLogo()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Isposed Manager",
                style = MiuixTheme.textStyles.headline2,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "v${packageInfo.versionName}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_tagline),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
        }

        SectionTitle(stringResource(R.string.about_section_info))
        SectionCard {
            InfoRow(stringResource(R.string.about_version_name), packageInfo.versionName)
            InfoRow(stringResource(R.string.about_version_code), packageInfo.versionCode)
            InfoRow(stringResource(R.string.about_package_name), packageInfo.packageName)
        }

        SectionTitle(stringResource(R.string.about_signature_title))
        SectionCard {
            Text(
                text = stringResource(R.string.about_signature_desc),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            SpacerH(6)
            Text(
                text = packageInfo.signature,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
        }

        SectionTitle(stringResource(R.string.about_section_framework))
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LSPOSED_BRAND),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.lsposed_ic_launcher_foreground),
                        contentDescription = "LSPosed",
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.about_framework_title),
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.about_framework_sub),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
            SpacerH(10)
            Text(
                text = stringResource(R.string.about_framework_desc),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            SpacerH(8)
            Text(
                text = stringResource(R.string.about_framework_desc2),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }

        SectionTitle(stringResource(R.string.about_section_features))
        SectionCard {
            rewrittenFeatures().forEach { f ->
                FeatureRow(f)
            }
        }

        SectionTitle(stringResource(R.string.about_section_author))
        SectionCard {
            Text(
                text = stringResource(R.string.about_author_name),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            SpacerH(6)
            Text(
                text = stringResource(R.string.about_author_bilibili),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.about_author_github),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }

        SectionTitle(stringResource(R.string.about_section_license))
        SectionCard {
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.about_license_expand),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    SpacerH(8)
                    Text(
                        text = stringResource(R.string.about_license_text),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
        }
        SpacerH(28)
    }
}

@Composable
private fun rewrittenFeatures(): List<RewrittenFeature> = listOf(
    RewrittenFeature(
        Icons.Filled.Build,
        stringResource(R.string.about_feature_1_title),
        stringResource(R.string.about_feature_1_desc),
    ),
    RewrittenFeature(
        Icons.Filled.CheckCircle,
        stringResource(R.string.about_feature_2_title),
        stringResource(R.string.about_feature_2_desc),
    ),
    RewrittenFeature(
        Icons.Filled.RocketLaunch,
        stringResource(R.string.about_feature_3_title),
        stringResource(R.string.about_feature_3_desc),
    ),
    RewrittenFeature(
        Icons.Filled.Terminal,
        stringResource(R.string.about_feature_4_title),
        stringResource(R.string.about_feature_4_desc),
    ),
    RewrittenFeature(
        Icons.Filled.AutoAwesome,
        stringResource(R.string.about_feature_5_title),
        stringResource(R.string.about_feature_5_desc),
    ),
    RewrittenFeature(
        Icons.Filled.Code,
        stringResource(R.string.about_feature_6_title),
        stringResource(R.string.about_feature_6_desc),
    ),
    RewrittenFeature(
        Icons.Filled.Update,
        stringResource(R.string.about_feature_7_title),
        stringResource(R.string.about_feature_7_desc),
    ),
    RewrittenFeature(
        Icons.Filled.CheckCircle,
        stringResource(R.string.about_feature_8_title),
        stringResource(R.string.about_feature_8_desc),
    ),
)

@Composable
private fun PulsingLogo() {
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(LSPOSED_BRAND),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.lsposed_ic_launcher_foreground),
            contentDescription = "LSPosed",
            modifier = Modifier.size(72.dp * scale),
        )
    }
}

@Composable
private fun FeatureRow(f: RewrittenFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                f.icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(f.title, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Medium)
            Text(
                text = f.desc,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

private data class PackageInfo(
    val versionName: String,
    val versionCode: String,
    val packageName: String,
    val signature: String,
)

private fun packageInfo(context: android.content.Context): PackageInfo {
    return try {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toString()
        }
        val signature = signingFingerprint(info)
        PackageInfo(
            versionName = info.versionName ?: "1.0.8",
            versionCode = versionCode,
            packageName = context.packageName,
            signature = signature,
        )
    } catch (_: Throwable) {
        PackageInfo("1.0.8", "9", context.packageName, "")
    }
}

private fun signingFingerprint(info: android.content.pm.PackageInfo): String {
    return try {
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        val bytes = signatures?.firstOrNull()?.toByteArray() ?: return ""
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        digest.joinToString(":") { "%02X".format(it) }
    } catch (_: Throwable) {
        ""
    }
}
