package com.alexdremov.notate.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.alexdremov.notate.R

/**
 * Loader/cacher for the AI Diary "Tom Riddle diary" handwritten typeface.
 *
 * The actual font file lives in res/font/ai_diary_handwritten.ttf.
 *
 * Current font: 书体坊赵九江钢笔行书, a Chinese pen-running-script font.
 * It provides full CJK coverage so the diary replies render as handwritten
 * Chinese text instead of falling back to the device's default KaiTi.
 *
 * Note: this font is provided by 书体坊 (Shutifang). For commercial use,
 * please contact the copyright holder to obtain a proper license.
 * See assets/ReadMe-ShutifangZhaojiujiang.txt for the distributor readme.
 */
object AiDiaryTypeface {
    const val NAME = "ai_diary_handwritten"

    @Volatile
    private var cachedTypeface: Typeface? = null

    fun get(context: Context): Typeface? {
        cachedTypeface?.let { return it }
        return synchronized(this) {
            cachedTypeface ?: run {
                try {
                    ResourcesCompat.getFont(context.applicationContext, R.font.ai_diary_handwritten)
                } catch (e: Exception) {
                    null
                }?.also { cachedTypeface = it }
            }
        }
    }

    fun clear() {
        cachedTypeface = null
    }
}
