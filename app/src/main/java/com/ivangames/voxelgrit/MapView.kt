package com.ivangames.voxelgrit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class MapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mapBitmap: Bitmap? = null
    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var scaleDetector: ScaleGestureDetector

    private val dotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val dotOutlinePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val testDots = listOf(
        floatArrayOf(300f, 200f),
        floatArrayOf(800f, 250f),
        floatArrayOf(1000f, 300f),
        floatArrayOf(900f, 500f)
    )

    init {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
                invalidate()
                return true
            }
        })
        loadMapImage()
    }

    private fun loadMapImage() {
        try {
            val resId = resources.getIdentifier("map", "drawable", context.packageName)
            if (resId != 0) {
                mapBitmap = BitmapFactory.decodeResource(resources, resId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        canvas.translate(offsetX, offsetY)

        mapBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        val dotRadius = 5f / scaleFactor
        for (dot in testDots) {
            val x = dot[0]
            val y = dot[1]
            canvas.drawCircle(x, y, dotRadius + 1f, dotOutlinePaint)
            canvas.drawCircle(x, y, dotRadius, dotPaint)
        }

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    offsetX += dx
                    offsetY += dy
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (!isDragging) {
                    // Здесь будет обработка клика
                }
            }
        }
        return true
    }

    fun zoomIn() {
        scaleFactor *= 1.2f
        scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
        invalidate()
    }

    fun zoomOut() {
        scaleFactor /= 1.2f
        scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
        invalidate()
    }
}
