package com.wangucanghai.isposed.ui.notice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.SpacerH

/**
 * Mandatory first-launch agreement gate. Blocks the whole app until every
 * notice (user agreement, open-source notice, AI notice) is acknowledged.
 *
 * Copyright (C) 2026 万古沧海
 */
@Composable
fun AgreementGate(
    prefs: AppPrefs,
    onAccepted: () -> Unit,
) {
    // Consume back so the gate cannot be dismissed by gesture/back button.
    BackHandler {}

    var agreedAll by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf(false) }
    var oss by remember { mutableStateOf(false) }
    var ai by remember { mutableStateOf(false) }

    val canAccept = user && oss && ai

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SpacerH(24)
            Text(
                text = "欢迎使用 Isposed Manager",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            SpacerH(6)
            Text(
                text = "首次使用前，请阅读并同意以下全部协议。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpacerH(20)

            NoticeBlock(
                title = NoticeContent.USER_AGREEMENT_TITLE,
                text = NoticeContent.USER_AGREEMENT,
                checked = user,
                onToggle = { user = !user },
            )
            SpacerH(12)
            NoticeBlock(
                title = NoticeContent.OSS_NOTICE_TITLE,
                text = NoticeContent.OSS_NOTICE,
                checked = oss,
                onToggle = { oss = !oss },
            )
            SpacerH(12)
            NoticeBlock(
                title = NoticeContent.AI_NOTICE_TITLE,
                text = NoticeContent.AI_NOTICE,
                checked = ai,
                onToggle = { ai = !ai },
            )
            SpacerH(24)

            Button(
                onClick = {
                    agreedAll = true
                    prefs.agreementAccepted = true
                    prefs.agreementVersion = NoticeContent.AGREEMENT_VERSION
                    prefs.aiNoticeAccepted = true
                    onAccepted()
                },
                enabled = canAccept,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("我已阅读并同意以上全部协议")
            }
            SpacerH(12)
            Text(
                text = "不同意将无法使用本软件。作者：万古沧海（B站同名）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun NoticeBlock(
    title: String,
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            SpacerH(10)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
