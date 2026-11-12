package com.wangucanghai.isposed.ai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.wangucanghai.isposed.core.AppPrefs
import com.wangucanghai.isposed.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible chat API client. Supports pulling models and testing a model.
 *
 * Copyright (C) 2026 万古沧海
 */
class AiClient(private val context: Context) {

    private val prefs = AppPrefs(context)
    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = prefs.aiBaseUrl.isNotEmpty() && prefs.aiApiKey.isNotEmpty()

    /** Pull available models from the provider. */
    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val base = prefs.aiBaseUrl
        if (base.isEmpty()) return@withContext emptyList()
        val req = Request.Builder()
            .url("$base/models")
            .header("Authorization", "Bearer ${prefs.aiApiKey}")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Logger.e("listModels HTTP ${resp.code}")
                return@withContext emptyList()
            }
            val body = resp.body?.string() ?: return@withContext emptyList()
            val arr = JsonParser.parseString(body).asJsonObject.getAsJsonArray("data")
            arr.map { it.asJsonObject.get("id").asString }
        }
    }

    /** Test a model with a trivial prompt. Returns reply text. */
    suspend fun testModel(model: String): String = withContext(Dispatchers.IO) {
        val base = prefs.aiBaseUrl
        val payload = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "user", "content" to "Reply with exactly: OK")
            ),
            "temperature" to prefs.aiTemperature,
            "stream" to false,
        )
        val req = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer ${prefs.aiApiKey}")
            .post(gson.toJson(payload).toRequestBody(json))
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val err = try {
                    JsonParser.parseString(body).asJsonObject.getAsJsonObject("error")
                        .get("message").asString
                } catch (_: Exception) { body.take(120) }
                throw RuntimeException("HTTP ${resp.code}: $err")
            }
            val obj = JsonParser.parseString(body).asJsonObject
            obj.getAsJsonArray("choices")[0].asJsonObject
                .getAsJsonObject("message").get("content").asString
        }
    }

    /** Chat completion returning text. */
    suspend fun chat(system: String, user: String, model: String? = null): String =
        withContext(Dispatchers.IO) {
            val base = prefs.aiBaseUrl
            val m = model ?: prefs.aiModel.ifEmpty { "gpt-4o-mini" }
            val payload = mapOf(
                "model" to m,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to system),
                    mapOf("role" to "user", "content" to user),
                ),
                "temperature" to prefs.aiTemperature,
                "stream" to false,
            )
            val req = Request.Builder()
                .url("$base/chat/completions")
                .header("Authorization", "Bearer ${prefs.aiApiKey}")
                .post(gson.toJson(payload).toRequestBody(json))
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    val err = try {
                        JsonParser.parseString(body).asJsonObject.getAsJsonObject("error")
                            .get("message").asString
                    } catch (_: Exception) { body.take(160) }
                    throw RuntimeException("HTTP ${resp.code}: $err")
                }
                JsonParser.parseString(body).asJsonObject
                    .getAsJsonArray("choices")[0].asJsonObject
                    .getAsJsonObject("message").get("content").asString
            }
        }
}
