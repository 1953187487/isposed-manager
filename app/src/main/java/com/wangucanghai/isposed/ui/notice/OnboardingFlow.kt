package com.wangucanghai.isposed.ui.notice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.core.LocaleHelper
import com.wangucanghai.isposed.ui.components.SpacerH
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Mandatory onboarding flow: user agreement -> language -> open-source notice.
 *
 * Blocks the whole app until the user walks through every step. The flow runs
 * on first launch, whenever the agreement version changes, and right after an
 * in-app update finishes.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun OnboardingFlow(
    prefs: AppPrefs,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    // Consume back so the flow cannot be dismissed by gesture/back button.
    BackHandler {}

    var step by remember { mutableStateOf(0) }
    var agreed by remember { mutableStateOf(false) }
    var ossAccepted by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(prefs.appLanguage) }

    val progress by animateFloatAsState(
        targetValue = (step + 1) / 3f,
        animationSpec = tween(320),
        label = "onboardingProgress",
    )

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            SpacerH(20)
            Text(
                text = "欢迎使用 Isposed Manager",
                style = MiuixTheme.textStyles.headline2,
                color = MiuixTheme.colorScheme.onBackground,
            )
            SpacerH(6)
            Text(
                text = "完成以下 ${3} 步即可开始使用",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            SpacerH(12)
            ProgressTrack(progress = progress)
            SpacerH(16)

            // The step area must carry the remaining weight, otherwise the scrollable
            // content measures with an unbounded height and pushes the action row
            // below the viewport.
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)))
                        .togetherWith(
                            slideOutHorizontally(tween(220)) { -it / 3 } + fadeOut(tween(220)),
                        )
                },
                label = "onboardingStep",
            ) { current ->
                when (current) {
                    0 -> AgreementStep(
                        title = NoticeContent.USER_AGREEMENT_TITLE,
                        text = NoticeContent.USER_AGREEMENT,
                        checked = agreed,
                        onToggle = { agreed = !agreed },
                    )

                    1 -> LanguageStep(
                        selected = language,
                        onSelect = { language = it },
                    )

                    else -> OssStep(
                        text = NoticeContent.OSS_NOTICE,
                        checked = ossAccepted,
                        onToggle = { ossAccepted = !ossAccepted },
                    )
                }
            }

            SpacerH(16)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    TextButton(
                        text = "上一步",
                        onClick = { step -= 1 },
                        modifier = Modifier.weight(1f),
                        cornerRadius = 14.dp,
                    )
                }
                Button(
                    onClick = {
                        when (step) {
                            0 -> {
                                agreed = true
                                step = 1
                            }

                            1 -> step = 2

                            else -> {
                                ossAccepted = true
                                prefs.agreementAccepted = true
                                prefs.agreementVersion = NoticeContent.AGREEMENT_VERSION
                                prefs.aiNoticeAccepted = true
                                if (language != prefs.appLanguage) {
                                    prefs.appLanguage = language
                                    LocaleHelper.applyToSystem(context, language)
                                }
                                onFinished()
                            }
                        }
                    },
                    enabled = true,
                    modifier = Modifier.weight(if (step > 0) 1.6f else 1f),
                    minHeight = 50.dp,
                    cornerRadius = 14.dp,
                ) {
                    Text(
                        when (step) {
                            0 -> "同意并继续"
                            1 -> "下一步"
                            else -> "同意并开始使用"
                        },
                    )
                }
            }
            SpacerH(10)
            Text(
                text = "不同意将无法使用本软件。作者：万古沧海（B站同名）",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .padding(2.dp),
        ) {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun AgreementStep(
    title: String,
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        NoticeBlock(title = title, text = text)
        SpacerH(12)
        AgreeRow(
            label = "我已阅读并同意《${title}》",
            checked = checked,
            onToggle = onToggle,
        )
        SpacerH(12)
    }
}

@Composable
private fun OssStep(
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        NoticeBlock(
            title = NoticeContent.OSS_NOTICE_TITLE,
            text = text,
        )
        SpacerH(12)
        AgreeRow(
            label = "我已阅读并同意《${NoticeContent.OSS_NOTICE_TITLE}》",
            checked = checked,
            onToggle = onToggle,
        )
        SpacerH(12)
    }
}

private data class LanguageOption(
    val code: String,
    val title: String,
    val desc: String,
)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption(LocaleHelper.LANGUAGE_SYSTEM, "跟随系统", "使用设备当前语言"),
    LanguageOption(LocaleHelper.LANGUAGE_ZH, "简体中文", "中文界面"),
    LanguageOption(LocaleHelper.LANGUAGE_EN, "English", "English interface"),
)

@Composable
private fun LanguageStep(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = "选择界面语言",
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onSurface,
        )
        SpacerH(4)
        Text(
            text = "语言设置随时可在设置中调整。",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        SpacerH(12)
        LANGUAGE_OPTIONS.forEach { option ->
            NoticeBlock(
                title = option.title,
                text = option.desc,
                checked = selected == option.code,
                onToggle = { onSelect(option.code) },
                leadingIcon = Icons.Filled.Language,
            )
            SpacerH(10)
        }
    }
}

/**
 * Explicit agreement control. The checkbox owns the click handling so the tap
 * always reaches the toggle, and the trailing label stays readable.
 */
@Composable
private fun AgreeRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = onToggle,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.clickable { onToggle() },
        )
    }
}

@Composable
private fun NoticeBlock(
    title: String,
    text: String,
    checked: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Card(
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onToggle != null) Modifier.clickable { onToggle() } else Modifier,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (checked != null) {
                Icon(
                    imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (checked) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline,
                )
            }
        }
        SpacerH(10)
        Surface(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.7f)) {
            Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
