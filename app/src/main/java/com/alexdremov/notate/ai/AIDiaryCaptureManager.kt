package com.alexdremov.notate.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import com.alexdremov.notate.model.CanvasItem
import com.alexdremov.notate.model.InfiniteCanvasModel
import com.alexdremov.notate.model.Stroke
import com.alexdremov.notate.model.TextItem
import com.alexdremov.notate.ui.controller.CanvasController
import com.alexdremov.notate.ui.render.CanvasRenderer
import com.alexdremov.notate.ui.render.RenderQuality
import com.alexdremov.notate.util.AiDiaryTypeface
import com.alexdremov.notate.util.EpdFastModeController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIDiaryCaptureManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val canvasModel: InfiniteCanvasModel,
    private val canvasController: CanvasController,
    private val canvasRenderer: CanvasRenderer,
    private val onRequestRefresh: () -> Unit,
    private val onRequestPartialRefresh: (RectF?, UpdateMode) -> Unit,
    private val onStatusUpdate: (String) -> Unit,
    private val captureDelayMs: Long,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var captureRunnable: Runnable? = null
    private var fadeRunnable: Runnable? = null
    private var revealRunnable: Runnable? = null
    private var session: AIDiarySession = AIDiarySession()
    private var isProcessing = false

    /**
     * Strokes from the previous turn that should be faded out once the user
     * starts writing again. Kept visible so the user can read the reply
     * alongside their own handwriting.
     */
    private var pendingUserStrokes: List<Stroke> = emptyList()

    /**
     * The previous turn's reply. Faded out when the user starts a new stroke
     * to keep the diary page clean for the next exchange.
     */
    private var pendingReplyItem: TextItem? = null

    fun updateSession(newSession: AIDiarySession) {
        this.session = newSession
    }

    fun getSession(): AIDiarySession = session

    fun onStrokeStarted() {
        cancelPendingCapture()
        cancelFadeAnimation()
        cancelRevealAnimation()

        // When the user starts writing again, gracefully fade out both the
        // previous turn's handwriting and reply together in one animation pass.
        // Calling startFadeAnimation separately would cancel the first one,
        // so we merge them into a single list.
        val strokes = pendingUserStrokes
        val reply = pendingReplyItem
        pendingUserStrokes = emptyList()
        pendingReplyItem = null

        if (strokes.isEmpty() && reply == null) return

        val items = mutableListOf<CanvasItem>()
        strokes.forEach { items.add(it) }
        reply?.let { items.add(it) }

        if (items.isEmpty()) return

        val bounds = RectF()
        items.forEach { bounds.union(it.bounds) }
        startFadeAnimation(items, bounds)
    }

    fun onStrokeEnded() {
        if (isProcessing) return
        cancelPendingCapture()
        val runnable = Runnable { triggerCapture() }
        captureRunnable = runnable
        handler.postDelayed(runnable, captureDelayMs)
    }

    private fun cancelPendingCapture() {
        captureRunnable?.let { handler.removeCallbacks(it) }
        captureRunnable = null
    }

    private fun cancelFadeAnimation() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
    }

    private fun cancelRevealAnimation() {
        revealRunnable?.let { handler.removeCallbacks(it) }
        revealRunnable = null
    }

    private fun createApiClient(): AIDiaryApiClient? {
        val providerId = AIDiaryPreferences.getProviderId(context)
        val apiKey = AIDiaryPreferences.getApiKey(context, providerId)?.trim()
        if (apiKey.isNullOrBlank()) return null
        return AIDiaryApiClient(
            com.alexdremov.notate.ai.provider.ProviderSettings(
                baseUrl = AIDiaryPreferences.getBaseUrl(context),
                apiKey = apiKey,
                model = AIDiaryPreferences.getModel(context),
            ),
        )
    }

    private fun triggerCapture() {
        if (isProcessing) return

        val apiClient = createApiClient()
        if (apiClient == null) {
            onStatusUpdate("请先配置 API Key")
            return
        }

        isProcessing = true

        coroutineScope.launch(Dispatchers.Default) {
            val captureResult = captureHandwritingBitmap()
            if (captureResult == null) {
                isProcessing = false
                return@launch
            }
            val (bitmap, captureBounds) = captureResult

            // Collect user strokes inside the captured area so we can fade them
            // out once the user starts writing the next turn.
            val userStrokes = findUserStrokes(captureBounds)
            pendingUserStrokes = userStrokes

            withContext(Dispatchers.Main) {
                onStatusUpdate("墨水瓶在发光，日记本正在书写...")
                EpdFastModeController.applyTransientUpdate(UpdateMode.ANIMATION)
            }

            val userPrompt =
                "这是我刚才手写的内容。请根据图片中的文字语言，用同样的语言回复我。" +
                    "如果我写的是英文，请用英文回复；如果我写的是中文，请用中文回复。"
            // Only keep the last exchange to avoid historical language bias
            if (session.messages.size > 4) {
                val trimmed =
                    session.messages.takeLast(2).toMutableList()
                trimmed.add(
                    0,
                    AIDiaryMessage(role = "system", content = session.systemPrompt),
                )
                session.messages.clear()
                session.messages.addAll(trimmed)
            }
            session.messages.add(AIDiaryMessage(role = "user", content = userPrompt))

            val result = apiClient.sendMessage(session, bitmap)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { response ->
                        session.messages.add(AIDiaryMessage(role = "assistant", content = response))
                        insertResponseText(response, captureBounds)
                        onStatusUpdate("")
                        canvasModel.setConversationJson(session.toJson())
                    },
                    onFailure = { error ->
                        val msg = error.localizedMessage ?: "unknown"
                        onStatusUpdate("墨水干涸了: $msg")
                        canvasModel.setConversationJson(session.toJson())
                    },
                )
                isProcessing = false
            }
            bitmap.recycle()
        }
    }

    private suspend fun findUserStrokes(bounds: RectF): List<Stroke> {
        return canvasController
            .getItemsInRect(bounds)
            .filterIsInstance<Stroke>()
            .filter { it.opacity > 0.01f }
    }

    private fun startFadeAnimation(
        items: List<CanvasItem>,
        bounds: RectF,
    ) {
        if (items.isEmpty()) return
        cancelFadeAnimation()

        val steps = 8
        val stepDelayMs = 120L
        var currentStep = 0

        val runnable =
            object : Runnable {
                override fun run() {
                    currentStep++
                    val progress = currentStep / steps.toFloat()
                    val opacity = 1f - progress

                    coroutineScope.launch(Dispatchers.Default) {
                        canvasController.updateItemsOpacity(items, opacity)
                        withContext(Dispatchers.Main) {
                            onRequestPartialRefresh(bounds, UpdateMode.ANIMATION)
                        }
                    }

                    if (currentStep < steps) {
                        fadeRunnable = this
                        handler.postDelayed(this, stepDelayMs)
                    } else {
                        // Fully faded: remove items from the model
                        coroutineScope.launch(Dispatchers.Default) {
                            canvasModel.deleteItems(items)
                            withContext(Dispatchers.Main) {
                                onRequestPartialRefresh(bounds, UpdateMode.GC)
                            }
                        }
                        fadeRunnable = null
                    }
                }
            }
        fadeRunnable = runnable
        handler.post(runnable)
    }

    private fun startRevealAnimation(
        textItem: TextItem,
        bounds: RectF,
    ) {
        cancelRevealAnimation()

        // More steps + shorter delay = smoother ink-bleed reveal that feels
        // like handwriting slowly soaking into the parchment.
        val steps = 12
        val stepDelayMs = 70L
        var currentStep = 0

        val runnable =
            object : Runnable {
                override fun run() {
                    currentStep++
                    val linear = currentStep / steps.toFloat()
                    // Ease-out cubic: start slow/soft, then settle into crisp text.
                    val t = 1f - linear
                    val opacity = 1f - t * t * t

                    coroutineScope.launch(Dispatchers.Default) {
                        canvasController.updateItemsOpacity(listOf(textItem), opacity)
                        withContext(Dispatchers.Main) {
                            onRequestPartialRefresh(bounds, UpdateMode.ANIMATION)
                        }
                    }

                    if (currentStep < steps) {
                        revealRunnable = this
                        handler.postDelayed(this, stepDelayMs)
                    } else {
                        coroutineScope.launch(Dispatchers.Default) {
                            canvasController.updateItemsOpacity(listOf(textItem), 1f)
                            withContext(Dispatchers.Main) {
                                onRequestPartialRefresh(bounds, UpdateMode.GC)
                                onRequestRefresh()
                            }
                        }
                        revealRunnable = null
                    }
                }
            }
        revealRunnable = runnable
        handler.post(runnable)
    }

    private fun captureHandwritingBitmap(): Pair<Bitmap, RectF>? {
        val contentBounds = canvasModel.getContentBounds()
        if (contentBounds.isEmpty) return null

        val padding = 40f
        val bounds =
            RectF(
                contentBounds.left - padding,
                contentBounds.top - padding,
                contentBounds.right + padding,
                contentBounds.bottom + padding,
            )

        val width = (bounds.width()).toInt().coerceAtLeast(1)
        val height = (bounds.height()).toInt().coerceAtLeast(1)
        val maxDimension = 1024
        val scale =
            if (width > height) (maxDimension.toFloat() / width).coerceAtMost(1f)
            else (maxDimension.toFloat() / height).coerceAtMost(1f)

        val bitmapWidth = (width * scale).toInt().coerceAtLeast(1)
        val bitmapHeight = (height * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val matrix = Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(-bounds.left * scale, -bounds.top * scale)

        canvasRenderer.renderDirectVectorsSync(
            canvas = canvas,
            matrix = matrix,
            visibleRect = bounds,
            quality = RenderQuality.HIGH,
        )

        return bitmap to bounds
    }

    private suspend fun insertResponseText(
        response: String,
        captureBounds: RectF,
    ) {
        // Position the reply near the user's handwriting, not far below.
        // If there's space to the right, place it there; otherwise below.
        val margin = 60f

        // Decide side: if the capture area is narrow (portrait note), place below;
        // otherwise try right to keep the reply close to the ink.
        val placeToRight = captureBounds.width() > captureBounds.height() * 1.6f

        val x: Float
        val y: Float
        if (placeToRight) {
            x = captureBounds.right + margin
            y = captureBounds.top
        } else {
            x = captureBounds.left.coerceAtLeast(0f)
            y = captureBounds.bottom + margin
        }

        val replyItem =
            canvasController.addText(
                text = response,
                x = x,
                y = y,
                fontSize = 48f,
                color = android.graphics.Color.BLACK,
                typefaceName = AiDiaryTypeface.NAME,
                selectAfterAdd = false,
            )

        if (replyItem != null) {
            // Start with fully transparent so we can reveal it with ink-bleed
            pendingReplyItem = replyItem
            canvasController.updateItemsOpacity(listOf(replyItem), 0f)
            val replyBounds = RectF(replyItem.bounds)
            startRevealAnimation(replyItem, replyBounds)
        } else {
            onRequestRefresh()
        }
    }

    fun destroy() {
        cancelPendingCapture()
        cancelFadeAnimation()
        cancelRevealAnimation()
        handler.removeCallbacksAndMessages(null)
    }
}
