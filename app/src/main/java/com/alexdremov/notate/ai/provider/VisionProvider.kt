package com.alexdremov.notate.ai.provider

import android.graphics.Bitmap

/**
 * Provider descriptor for UI. Not tied to any runtime class.
 */
data class ProviderDescriptor(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val note: String = "",
    val supportsImage: Boolean? = null,
)

/**
 * Settings used when making a request.
 */
data class ProviderSettings(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)

sealed class TestResult {
    data class Success(val summary: String) : TestResult()
    data class Failure(val category: TestFailureCategory, val message: String) : TestResult()
}

enum class TestFailureCategory {
    NETWORK,
    AUTH,
    BAD_REQUEST,
    OTHER,
}

interface VisionProvider {
    val id: String
    val displayName: String
    val descriptor: ProviderDescriptor

    suspend fun chat(
        imageBitmap: Bitmap,
        textPrompt: String,
        systemPrompt: String,
        settings: ProviderSettings,
    ): Result<String>

    suspend fun testConnection(settings: ProviderSettings): TestResult
}
