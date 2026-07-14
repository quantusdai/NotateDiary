package com.alexdremov.notate.ui.render.background

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.random.Random

/**
 * Renders an old-book / parchment background for AI Diary mode.
 *
 * The effect consists of:
 * - a warm parchment base fill
 * - a low-alpha noise shader for paper grain
 * - a few deterministic ink stains seeded by the rendered area
 */
object ParchmentBackgroundRenderer {
    // Warm parchment tone (#F5E6C8)
    const val PARCHMENT_COLOR = 0xFFF5E6C8.toInt()

    private const val NOISE_SIZE = 256
    private const val STAIN_COUNT = 5

    private val paintProvider =
        ThreadLocal.withInitial {
            Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }

    @Volatile
    private var noiseBitmap: Bitmap? = null

    fun draw(
        canvas: Canvas,
        rect: RectF,
    ) {
        if (rect.isEmpty) return

        val paint = paintProvider.get()!!

        // 1. Warm base fill
        paint.shader = null
        paint.color = PARCHMENT_COLOR
        canvas.drawRect(rect, paint)

        // 2. Subtle paper-grain noise using a repeating shader
        val noise = getNoiseBitmap()
        val savedShader = paint.shader
        paint.shader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        paint.alpha = 18
        canvas.drawRect(rect, paint)
        paint.alpha = 255
        paint.shader = savedShader

        // 3. Deterministic ink stains along the edges
        drawInkStains(canvas, rect, paint)
    }

    private fun getNoiseBitmap(): Bitmap {
        noiseBitmap?.let { return it }
        return synchronized(this) {
            noiseBitmap ?: createNoiseBitmap().also { noiseBitmap = it }
        }
    }

    private fun createNoiseBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(NOISE_SIZE, NOISE_SIZE, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(NOISE_SIZE * NOISE_SIZE)
        for (i in pixels.indices) {
            // Slight lightness variation around mid-gray
            val value = (160 + (Math.random() * 40).toInt()).coerceIn(0, 255)
            pixels[i] = (0x30 shl 24) or (value shl 16) or (value shl 8) or value
        }
        bitmap.setPixels(pixels, 0, NOISE_SIZE, 0, 0, NOISE_SIZE, NOISE_SIZE)
        return bitmap
    }

    private fun drawInkStains(
        canvas: Canvas,
        rect: RectF,
        paint: Paint,
    ) {
        val seed = (rect.left.toInt() * 73856093) xor (rect.top.toInt() * 19349663)
        val random = Random(seed)

        paint.shader = null
        paint.color = Color.BLACK

        repeat(STAIN_COUNT) {
            val cx = rect.left + random.nextFloat() * rect.width()
            val cy = rect.top + random.nextFloat() * rect.height()
            val radius = 20f + random.nextFloat() * 60f
            val alpha = (6 + random.nextFloat() * 10).toInt().coerceIn(0, 255)
            paint.alpha = alpha
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.alpha = 255
    }
}
