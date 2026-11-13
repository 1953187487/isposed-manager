package com.wangucanghai.isposed.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ai.AiClient
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.notice.NoticeContent
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AI service provider config: pull models & test model.
 *
 * Copyright (C) 2026 万古沧海
 */
class AiConfigActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = stringResource(R.string.ai_config), onBack = { finish() }) {
                    AiConfigContent()
                }
            }
        }
    }
}

@Composable
private fun AiConfigContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPrefs(context) }
    val client = remember { AiClient(context) }

    var baseUrl by remember { mutableStateOf(TextFieldValue(prefs.aiBaseUrl)) }
    var apiKey by remember { mutableStateOf(TextFieldValue(prefs.aiApiKey)) }
    var model by remember { mutableStateOf(TextFieldValue(prefs.aiModel)) }
    var temp by remember { mutableFloatStateOf(prefs.aiTemperature) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    SectionCard {
        TextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.ai_base_url),
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        TextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.ai_api_key),
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        TextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.ai_model),
            singleLine = true,
            cornerRadius = 14.dp,
        )
        SpacerH(10)
        Text(
            text = stringResource(R.string.ai_temperature, "%.2f".format(temp)),
            style = MiuixTheme.textStyles.footnote1,
        )
        Slider(
            value = temp,
            onValueChange = { temp = it },
            valueRange = 0f..2f,
            steps = 8,
        )
    }
    SpacerH(16)
    Button(
        onClick = {
            prefs.aiBaseUrl = baseUrl.text.trim()
            prefs.aiApiKey = apiKey.text.trim()
            prefs.aiModel = model.text.trim()
            prefs.aiTemperature = temp
            Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show()
            Logger.i("AI config saved")
        },
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.ai_save))
    }
    SpacerH(10)
    Button(
        onClick = {
            prefs.aiBaseUrl = baseUrl.text.trim()
            prefs.aiApiKey = apiKey.text.trim()
            result = context.getString(R.string.ai_pulling)
            busy = true
            scope.launch {
                val models = withContext(Dispatchers.IO) { runCatching { client.listModels() }.getOrDefault(emptyList()) }
                result = if (models.isEmpty()) context.getString(R.string.ai_pull_failed)
                else context.getString(R.string.ai_model_list, models.joinToString("\n"))
                if (model.text.isBlank()) model = TextFieldValue(models.firstOrNull().orEmpty())
                busy = false
            }
        },
        enabled = !busy,
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.ai_pull_models))
    }
    SpacerH(10)
    Button(
        onClick = {
            prefs.aiBaseUrl = baseUrl.text.trim()
            prefs.aiApiKey = apiKey.text.trim()
            prefs.aiModel = model.text.trim()
            val m = model.text.trim()
            if (m.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.ai_model_required), Toast.LENGTH_SHORT).show()
                return@Button
            }
            result = context.getString(R.string.ai_testing)
            busy = true
            scope.launch {
                val r = withContext(Dispatchers.IO) { runCatching { client.testModel(m) } }
                result = r.fold(
                    onSuccess = { context.getString(R.string.ai_test_ok, it) },
                    onFailure = { context.getString(R.string.ai_test_failed, it.message ?: "") },
                )
                busy = false
            }
        },
        enabled = !busy,
        cornerRadius = 14.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.ai_test_model))
    }
    if (result.isNotEmpty()) {
        SpacerH(16)
        SectionCard {
            Text(
                result,
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
    SpacerH(16)
    SectionTitle(stringResource(R.string.notice_ai_title))
    SectionCard {
        Text(
            text = stringResource(R.string.notice_ai_body),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
    SpacerH(28)
}
