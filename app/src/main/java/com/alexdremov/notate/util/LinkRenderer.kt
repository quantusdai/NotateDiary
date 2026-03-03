package com.alexdremov.notate.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.core.content.ContextCompat
import com.alexdremov.notate.R
import com.alexdremov.notate.data.LinkType
import com.alexdremov.notate.model.LinkItem

object LinkRenderer {
    private const val BASE_FONT_SIZE = 20f
    private const val BASE_PADDING_X = 20f
    private const val BASE_PADDING_Y = 10f
    private const val BASE_ICON_SIZE = 24f
    private const val BASE_ICON_PADDING = 10f
    private const val BASE_CORNER_RADIUS = 12f

    private fun getScale(fontSize: Float) = fontSize / BASE_FONT_SIZE

    fun measureSize(
        context: Context,
        label: String,
        fontSize: Float,
    ): Pair<Float, Float> {
        val scale = getScale(fontSize)
        val paint = Paint()
        paint.textSize = fontSize
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        val textWidth = paint.measureText(label)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val totalWidth = (BASE_PADDING_X * 2 + BASE_ICON_SIZE + BASE_ICON_PADDING) * scale + textWidth
        val totalHeight = kotlin.math.max(textHeight, BASE_ICON_SIZE * scale) + (BASE_PADDING_Y * 2) * scale

        return Pair(totalWidth, totalHeight)
    }

    fun draw(
        canvas: Canvas,
        item: LinkItem,
        context: Context?,
        paint: Paint,
        canvasScale: Float = 1.0f,
    ) {
        val originalStyle = paint.style
        val originalColor = paint.color
        val originalStrokeWidth = paint.strokeWidth
        val xfermode = paint.xfermode

        val scale = getScale(item.fontSize)
        val cornerRadius = BASE_CORNER_RADIUS * scale

        // Apply rotation around center
        canvas.save()
        val centerX = item.logicalBounds.centerX()
        val centerY = item.logicalBounds.centerY()
        canvas.rotate(item.rotation, centerX, centerY)

        // Draw Background (White capsule)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        // Shadow effect could be added here if needed
        canvas.drawRoundRect(item.logicalBounds, cornerRadius, cornerRadius, paint)

        // If we are in a clearing/masking mode (e.g. from hideItemsInCache or Eraser),
        // the background fill above already cleared/masked the area.
        // We MUST NOT draw the icon or text as they might not respect the xfermode (Drawables)
        // or would just be redundant work.
        if (xfermode == null) {
            // Draw Border (Non-Scaling Stroke)
            paint.style = Paint.Style.STROKE
            paint.color = item.color
            val effectiveScale = if (canvasScale > 0) canvasScale else 1.0f
            paint.strokeWidth = 2f / effectiveScale
            canvas.drawRoundRect(item.logicalBounds, cornerRadius, cornerRadius, paint)

            // Draw Icon and Text
            val contentLeft = item.logicalBounds.left + BASE_PADDING_X * scale
            val contentCenterY = item.logicalBounds.centerY()
            val iconSize = BASE_ICON_SIZE * scale

            // Icon
            drawIcon(canvas, item.type, contentLeft, contentCenterY, iconSize, paint, context, item.color)

            // Text
            val textLeft = contentLeft + iconSize + BASE_ICON_PADDING * scale
            paint.style = Paint.Style.FILL
            paint.color = item.color // Text same color as border/icon
            paint.textSize = item.fontSize
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            // Align text vertically
            val fontMetrics = paint.fontMetrics
            val textY = contentCenterY - (fontMetrics.descent + fontMetrics.ascent) / 2
            canvas.drawText(item.label, textLeft, textY, paint)
        } else {
            // Also clear the border area if we are in CLEAR mode
            paint.style = Paint.Style.STROKE
            val effectiveScale = if (canvasScale > 0) canvasScale else 1.0f
            paint.strokeWidth = 2f / effectiveScale
            canvas.drawRoundRect(item.logicalBounds, cornerRadius, cornerRadius, paint)
        }

        // Restore paint and canvas
        paint.style = originalStyle
        paint.color = originalColor
        paint.strokeWidth = originalStrokeWidth
        canvas.restore()
    }

    private fun drawIcon(
        canvas: Canvas,
        type: LinkType,
        x: Float,
        y: Float,
        size: Float,
        paint: Paint,
        context: Context?,
        color: Int,
    ) {
        if (context == null) return

        val drawableId =
            when (type) {
                LinkType.INTERNAL_NOTE -> R.drawable.ic_link_file
                LinkType.EXTERNAL_URL -> R.drawable.ic_link_globe
                LinkType.LOCAL_FILE -> R.drawable.ic_link_file
            }

        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return

        drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)

        val left = x.toInt()
        val top = (y - size / 2).toInt()
        val right = (x + size).toInt()
        val bottom = (y + size / 2).toInt()

        drawable.setBounds(left, top, right, bottom)
        drawable.draw(canvas)
    }
}
