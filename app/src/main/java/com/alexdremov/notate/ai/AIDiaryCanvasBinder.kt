package com.alexdremov.notate.ai

import android.graphics.RectF
import android.content.Context
import com.alexdremov.notate.model.InfiniteCanvasModel
import com.alexdremov.notate.ui.controller.CanvasController
import com.alexdremov.notate.ui.render.CanvasRenderer
import com.onyx.android.sdk.api.device.epd.UpdateMode
import kotlinx.coroutines.CoroutineScope

/**
 * Lifecycle-friendly facade around [AIDiaryCaptureManager].
 *
 * The canvas UI (e.g. `CanvasActivity`) talks to this binder instead of the
 * manager directly: it owns the manager instance and simply forwards pen
 * stroke lifecycle events ([onStrokeStarted] / [onStrokeEnded]) and session
 * load/save. This keeps the AI-diary wiring in one place and makes the
 * manager easy to create, swap, and [destroy] with the host view.
 */
class AIDiaryCanvasBinder(
    context: Context,
    coroutineScope: CoroutineScope,
    canvasModel: InfiniteCanvasModel,
    canvasController: CanvasController,
    canvasRenderer: CanvasRenderer,
    onRequestRefresh: () -> Unit,
    onRequestPartialRefresh: (RectF?, UpdateMode) -> Unit,
    onStatusUpdate: (String) -> Unit,
    captureDelayMs: Long,
) {
    val captureManager =
        AIDiaryCaptureManager(
            context = context,
            coroutineScope = coroutineScope,
            canvasModel = canvasModel,
            canvasController = canvasController,
            canvasRenderer = canvasRenderer,
            onRequestRefresh = onRequestRefresh,
            onRequestPartialRefresh = onRequestPartialRefresh,
            onStatusUpdate = onStatusUpdate,
            captureDelayMs = captureDelayMs,
        )

    fun loadSession(session: AIDiarySession?) {
        session?.let { captureManager.updateSession(it) }
    }

    fun getCurrentSession(): AIDiarySession = captureManager.getSession()

    fun onStrokeStarted() {
        captureManager.onStrokeStarted()
    }

    fun onStrokeEnded() {
        captureManager.onStrokeEnded()
    }

    fun destroy() {
        captureManager.destroy()
    }
}
