package com.alexdremov.notate.ai.provider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import com.alexdremov.notate.ai.AIDiaryMessage
import com.alexdremov.notate.ai.AIDiarySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class OpenAiCompatProvider : VisionProvider {
    override val id: String = "openai_compat"
    override val displayName: String = "OpenAI 兼容"
    override val descriptor: ProviderDescriptor =
        ProviderDescriptor(
            id = id,
            displayName = displayName,
            defaultBaseUrl = "",
            defaultModel = "",
            note = "自定义 OpenAI 兼容服务",
            supportsImage = null,
        )

    private val mediaType = "application/json".toMediaType()

    private fun createClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    override suspend fun chat(
        imageBitmap: Bitmap,
        textPrompt: String,
        systemPrompt: String,
        settings: ProviderSettings,
    ): Result<String> =
        sendRequest(
            settings = settings,
            messages = buildMessages(systemPrompt, textPrompt, imageBitmap),
            maxTokens = 2048,
            temperature = 0.7,
        )

    override suspend fun testConnection(settings: ProviderSettings): TestResult =
        withContext(Dispatchers.IO) {
            val placeholder = create1x1Bitmap()
            val result =
                sendRequest(
                    settings = settings,
                    messages = buildMessages("", "这张图里有什么？用一句话回答。", placeholder),
                    maxTokens = 50,
                    temperature = 0.7,
                )
            placeholder.recycle()

            result.fold(
                onSuccess = { text -> TestResult.Success(text.take(80).replace("\n", " ")) },
                onFailure = { error ->
                    val msg = error.message ?: "unknown"
                    TestResult.Failure(classifyError(error, msg), msg)
                },
            )
        }

    private fun buildMessages(
        systemPrompt: String,
        textPrompt: String,
        imageBitmap: Bitmap,
    ): JsonArray {
        val imageBase64 = bitmapToBase64Png(imageBitmap)
        return buildJsonArray {
            if (systemPrompt.isNotBlank()) {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
            }
            addJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "image_url")
                        putJsonObject("image_url") {
                            put("url", "data:image/png;base64,$imageBase64")
                        }
                    }
                    addJsonObject {
                        put("type", "text")
                        put("text", textPrompt)
                    }
                }
            }
        }
    }

    private suspend fun sendRequest(
        settings: ProviderSettings,
        messages: JsonArray,
        maxTokens: Int,
        temperature: Double,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val baseUrl = settings.baseUrl.trimEnd('/')
                val url = "$baseUrl/chat/completions"

                val payload = buildJsonObject {
                    put("model", settings.model)
                    put("max_tokens", maxTokens)
                    put("temperature", temperature)
                    put("messages", messages)
                }

                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Authorization", "Bearer ${settings.apiKey.trim()}")
                        .header("Content-Type", "application/json")
                        .post(Json.encodeToString(JsonObject.serializer(), payload).toRequestBody(mediaType))
                        .build()

                createClient().newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            Exception("API error ${response.code}: $bodyString"),
                        )
                    }

                    val parsed =
                        try {
                            kotlinx.serialization.json.Json.parseToJsonElement(bodyString).jsonObject
                        } catch (e: Exception) {
                            return@use Result.failure(Exception("Invalid JSON: $bodyString"))
                        }

                    val content =
                        parsed["choices"]
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                            ?.get("message")
                            ?.jsonObject
                            ?.get("content")
                            ?.jsonPrimitive
                            ?.content

                    if (content.isNullOrBlank()) {
                        Result.failure(Exception("Empty response from AI"))
                    } else {
                        Result.success(content.trim { it <= ' ' })
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun classifyError(error: Throwable, message: String): TestFailureCategory =
        when {
            error is UnknownHostException ||
                error is ConnectException ||
                error is SocketTimeoutException -> TestFailureCategory.NETWORK
            message.contains("401", ignoreCase = true) -> TestFailureCategory.AUTH
            message.contains("400", ignoreCase = true) ||
                message.contains("unknown variant image_url", ignoreCase = true) ||
                message.contains("image_url", ignoreCase = true) -> TestFailureCategory.BAD_REQUEST
            else -> TestFailureCategory.OTHER
        }

    private fun bitmapToBase64Png(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun create1x1Bitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        return bitmap
    }
}
