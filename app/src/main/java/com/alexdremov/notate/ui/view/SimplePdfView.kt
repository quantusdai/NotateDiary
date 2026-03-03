package com.alexdremov.notate.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.LruCache
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexdremov.notate.util.Logger
import java.io.File

class SimplePdfView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val recyclerView: RecyclerView =
            RecyclerView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                layoutManager = LinearLayoutManager(context)
                setBackgroundColor(Color.WHITE)
            }

        private var pdfRenderer: PdfRenderer? = null
        private var fileDescriptor: ParcelFileDescriptor? = null
        private val bitmapCache =
            object : LruCache<Int, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
                override fun sizeOf(
                    key: Int,
                    value: Bitmap,
                ): Int = value.byteCount
            }

        init {
            addView(recyclerView)
        }

        fun setPdfFile(file: File) {
            closeRenderer()
            try {
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                recyclerView.adapter = PdfPageAdapter()
            } catch (e: Exception) {
                Logger.e("SimplePdfView", "Failed to load PDF: ${file.path}", e)
            }
        }

        private fun closeRenderer() {
            bitmapCache.evictAll()
            recyclerView.adapter = null
            pdfRenderer?.close()
            fileDescriptor?.close()
            pdfRenderer = null
            fileDescriptor = null
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            closeRenderer()
        }

        private inner class PdfPageAdapter : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): PageViewHolder {
                val imageView =
                    ImageView(context).apply {
                        layoutParams =
                            RecyclerView.LayoutParams(
                                RecyclerView.LayoutParams.MATCH_PARENT,
                                RecyclerView.LayoutParams.WRAP_CONTENT,
                            )
                        adjustViewBounds = true
                        setPadding(0, 0, 0, 16)
                    }
                return PageViewHolder(imageView)
            }

            override fun onBindViewHolder(
                holder: PageViewHolder,
                position: Int,
            ) {
                val renderer = pdfRenderer ?: return
                var bitmap = bitmapCache.get(position)

                if (bitmap == null) {
                    val page = renderer.openPage(position)
                    // Use a reasonable width for E-Ink, typically around screen width
                    val targetWidth =
                        context.resources.displayMetrics.widthPixels
                            .coerceAtMost(1200)
                    val targetHeight = (targetWidth * (page.height.toFloat() / page.width)).toInt()

                    try {
                        bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmapCache.put(position, bitmap)
                    } catch (e: OutOfMemoryError) {
                        Logger.e("SimplePdfView", "OOM while rendering page $position")
                    } finally {
                        page.close()
                    }
                }

                (holder.itemView as ImageView).setImageBitmap(bitmap)
            }

            override fun getItemCount(): Int = pdfRenderer?.pageCount ?: 0

            inner class PageViewHolder(
                view: android.view.View,
            ) : RecyclerView.ViewHolder(view)
        }
    }
