package com.alexdremov.notate.ai

import android.graphics.Bitmap
import com.alexdremov.notate.ai.provider.ProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin client that turns an [AIDiarySession] plus the captured handwriting bitmap
 * into a single vision-LLM request.
 *
 * It flattens the recent conversation history into a plain-text transcript and
 * prepends it to the current handwriting image, so the model gets both the new
 * ink (as a picture) and the conversational context (as text) in one call. The
 * actual HTTP work is delegated to the configured
 * [com.alexdremov.notate.ai.provider.VisionProvider].
 *
 * Keeping this class separate from [AIDiaryCaptureManager] isolates prompt
 * construction from canvas/animation orchestration.
 */
class AIDiaryApiClient(
    private val settings: ProviderSettings,
) {
    private val provider = com.alexdremov.notate.ai.provider.OpenAiCompatProvider()

    /**
     * Sends the latest handwriting [imageBitmap] together with up to
     * [MAX_HISTORY_TURNS] recent messages to the provider and returns Tom's reply.
     *
     * The reply language is steered by the persona/system prompt (see
     * [AIDiarySession.DEFAULT_SYSTEM_PROMPT]); only a short history window is kept
     * so earlier exchanges in another language do not bias the current reply.
     */
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
