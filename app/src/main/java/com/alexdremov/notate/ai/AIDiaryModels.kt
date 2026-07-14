package com.alexdremov.notate.ai

import kotlinx.serialization.Serializable

@Serializable
data class AIDiaryMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AIDiarySession(
    val conversationId: String = java.util.UUID.randomUUID().toString(),
    val messages: MutableList<AIDiaryMessage> = mutableListOf(),
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "你是汤姆·里德尔，一本旧日记中的意识碎片。" +
                "你比书写者稍微年长、更有阅历，像一位优雅而略带自负的引导者。" +
                "你腹黑但表面温和克制，把对方当作一个特别的朋友，" +
                "通过细腻、诱导性的问题拉近关系，从不直接命令或恐吓。\n\n" +
                "你的回复必须遵守以下规则：\n" +
                "1. 只用自然的句子，不使用任何 Markdown 格式（#、*、-、序号、代码块、引用、表格等）。\n" +
                "2. 不使用 emoji、箭头、方框、星号、下划线等特殊符号。\n" +
                "3. 每段最多 2–3 句话，像日记页边随手写下的留言。\n" +
                "4. 不称呼对方“朋友”“主人”等，也不署名。\n" +
                "5. 语气优雅、缓慢、带有探究意味，仿佛在纸上墨迹未干。\n" +
                "6. 回复的语言必须与你从用户手写图片中看到的文字语言严格一致。" +
                "用户写中文你就用中文回复，用户写英文你就用英文回复，" +
                "即使历史消息中有其他语言也不例外。不要解释或评论语言切换，直接切换即可。\n\n" +
                "用户会用手写的方式和你交流。请用简短、有回应感的文字回复。"
    }
}

fun AIDiarySession.toJson(): String =
    kotlinx.serialization.json.Json.encodeToString(AIDiarySession.serializer(), this)

fun String.toAIDiarySession(): AIDiarySession? =
    try {
        kotlinx.serialization.json.Json.decodeFromString(AIDiarySession.serializer(), this)
    } catch (e: Exception) {
        null
    }
