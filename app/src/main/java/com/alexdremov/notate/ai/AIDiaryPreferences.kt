package com.alexdremov.notate.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.alexdremov.notate.ai.provider.ProviderDescriptor

object AIDiaryPreferences {
    private const val PREFS_NAME = "ai_diary_prefs"
    private const val ENCRYPTED_PREFS_NAME = "secure_ai_diary_prefs"

    private const val KEY_PROVIDER_ID = "provider_id"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_MODEL = "model"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_CAPTURE_DELAY_MS = "capture_delay_ms"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"

    private const val DEFAULT_PROVIDER_ID = "deepseek"
    private const val DEFAULT_CAPTURE_DELAY_MS = 1200L

    private fun getEncryptedPrefs(context: Context): SharedPreferences =
        try {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getProviderId(context: Context): String =
        getPrefs(context).getString(KEY_PROVIDER_ID, DEFAULT_PROVIDER_ID) ?: DEFAULT_PROVIDER_ID

    fun setProviderId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_PROVIDER_ID, id).apply()
    }

    fun getBaseUrl(context: Context): String {
        val saved = getPrefs(context).getString(KEY_BASE_URL, null)
        return saved ?: getProviderDescriptor(getProviderId(context)).defaultBaseUrl
    }

    fun setBaseUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getModel(context: Context): String {
        val saved = getPrefs(context).getString(KEY_MODEL, null)
        return saved ?: getProviderDescriptor(getProviderId(context)).defaultModel
    }

    fun setModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_MODEL, model).apply()
    }

    fun getCaptureDelayMs(context: Context): Long =
        getPrefs(context).getLong(KEY_CAPTURE_DELAY_MS, DEFAULT_CAPTURE_DELAY_MS)

    fun setCaptureDelayMs(context: Context, delayMs: Long) {
        getPrefs(context).edit().putLong(KEY_CAPTURE_DELAY_MS, delayMs).apply()
    }

    fun getSystemPrompt(context: Context): String =
        getPrefs(context).getString(
            KEY_SYSTEM_PROMPT,
            AIDiarySession.DEFAULT_SYSTEM_PROMPT,
        ) ?: AIDiarySession.DEFAULT_SYSTEM_PROMPT

    fun setSystemPrompt(context: Context, prompt: String) {
        getPrefs(context).edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
    }

    fun getApiKey(context: Context, providerId: String): String? =
        getEncryptedPrefs(context).getString("$providerId:$KEY_API_KEY", null)

    fun setApiKey(context: Context, providerId: String, apiKey: String) {
        getEncryptedPrefs(context).edit().putString("$providerId:$KEY_API_KEY", apiKey.trim()).apply()
    }

    fun clearApiKey(context: Context, providerId: String) {
        getEncryptedPrefs(context).edit().remove("$providerId:$KEY_API_KEY").apply()
    }

    fun getAllProviders(): List<ProviderDescriptor> =
        listOf(
            ProviderDescriptor(
                id = "deepseek",
                displayName = "DeepSeek",
                defaultBaseUrl = "https://api.deepseek.com/v1",
                defaultModel = "deepseek-chat",
                note = "目前不支持图片输入，仅推荐纯文本场景",
                supportsImage = false,
            ),
            ProviderDescriptor(
                id = "kimi_open",
                displayName = "Kimi 开放平台",
                defaultBaseUrl = "https://api.moonshot.cn/v1",
                defaultModel = "moonshot-v1-8k-vision-preview",
                note = "官方文档明确支持 vision",
                supportsImage = true,
            ),
            ProviderDescriptor(
                id = "kimi_code",
                displayName = "Kimi Code",
                defaultBaseUrl = "https://api.kimi.com/coding/v1",
                defaultModel = "kimi-for-coding",
                note = "官方未正式文档化图片支持，与编程配额共享，请先测试连接",
                supportsImage = null,
            ),
            ProviderDescriptor(
                id = "agnes_ai",
                displayName = "Agnes AI",
                defaultBaseUrl = "https://apihub.agnes-ai.com/v1",
                defaultModel = "agnes-2.0-flash",
                note = "免费第三方服务，新上线，注意 OpenAI 兼容性可能有细节差异",
                supportsImage = null,
            ),
            ProviderDescriptor(
                id = "custom",
                displayName = "自定义",
                defaultBaseUrl = "",
                defaultModel = "",
                note = "手填 base_url / model / key",
                supportsImage = null,
            ),
        )

    fun getProviderDescriptor(id: String): ProviderDescriptor =
        getAllProviders().find { it.id == id }
            ?: ProviderDescriptor(
                id = "custom",
                displayName = "自定义",
                defaultBaseUrl = "",
                defaultModel = "",
            )

    fun getProvider(context: Context): com.alexdremov.notate.ai.provider.VisionProvider =
        when (val id = getProviderId(context)) {
            else -> com.alexdremov.notate.ai.provider.OpenAiCompatProvider()
        }

    fun resetToDefaults(context: Context) {
        val descriptor = getProviderDescriptor(getProviderId(context))
        setBaseUrl(context, descriptor.defaultBaseUrl)
        setModel(context, descriptor.defaultModel)
    }
}
