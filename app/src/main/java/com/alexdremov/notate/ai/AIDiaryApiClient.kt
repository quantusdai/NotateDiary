package com.alexdremov.notate.ai

import android.graphics.Bitmap
import com.alexdremov.notate.ai.provider.ProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIDiaryApiClient(
    private val settings: ProviderSettings,
) {
    private val provider = com.alexdremov.notate.ai.provider.OpenAiCompatProvider()

    suspend fun sendMessage(
        session: AIDiarySession,
        imageBitmap: Bitmap,
    ): Result<String> {
        val messages = mutableListOf<AIDiaryMessage>()
        messages.add(AIDiaryMessage(role = "system", content = session.systemPrompt))
        messages.addAll(session.messages.takeLast(AIDiaryApiClient.MAX_HISTORY_TURNS))

        val conversationText = messages.joinToString("\n") { "${it.role}: ${it.content}" }

        return provider.chat(
            imageBitmap = imageBitmap,
            textPrompt = "这是我写的内容，请回复我。历史上下文：\n$conversationText",
            systemPrompt = session.systemPrompt,
            settings = settings,
        )
    }

    suspend fun testConnection(): com.alexdremov.notate.ai.provider.TestResult =
        withContext(Dispatchers.IO) {
            provider.testConnection(settings)
        }

    companion object {
        const val MAX_HISTORY_TURNS = 20
    }
}
