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
import kotlin.math.abs

class MapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mapBitmap: Bitmap? = null

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    // Для плавности
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTouchTime = 0L
    private var velocityX = 0f
    private var velocityY = 0f
    private var isDragging = false
    private var isAnimating = false
    private var scaleDetector: ScaleGestureDetector

    // Цели для плавного зума
    private var targetScale = 1f
    private val zoomAnimator = object : Runnable {
        override fun run() {
            if (abs(scaleFactor - targetScale) > 0.01f) {
                scaleFactor += (targetScale - scaleFactor) * 0.2f
                invalidate()
                postOnAnimation(this)
            } else {
                scaleFactor = targetScale
                invalidate()
            }
        }
    }

    // Кисть для отрисовки (не нужна, если нет фона, но оставим)
    private val paint = Paint().apply { isAntiAlias = true }

    init {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
                targetScale = scaleFactor
                invalidate()
                return true
            }
        })
        loadMap()
    }

    private fun loadMap() {
        try {
            // Загружаем картинку map.jpg из drawable
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
        
        // Фон (на случай если карта не прогрузится)
        canvas.drawColor(Color.parseColor("#87CEEB")) // Небесно-голубой

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        canvas.translate(offsetX, offsetY)

        // Рисуем картинку по центру
        mapBitmap?.let { bitmap ->
            val left = (width - bitmap.width) / 2f
            val top = (height - bitmap.height) / 2f
            canvas.drawBitmap(bitmap, left, top, paint)
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
                lastTouchTime = System.currentTimeMillis()
                velocityX = 0f
                velocityY = 0f
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    val dt = System.currentTimeMillis() - lastTouchTime
                    
                    offsetX += dx
                    offsetY += dy
                    
                    if (dt > 0) {
                        velocityX = dx / dt * 16f
                        velocityY = dy / dt * 16f
                    }
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    lastTouchTime = System.currentTimeMillis()
                    isDragging = true
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    if (abs(velocityX) > 0.5f || abs(velocityY) > 0.5f) {
                        isAnimating = true
                        postOnAnimation(object : Runnable {
                            override fun run() {
                                if (!isAnimating) return
                                offsetX += velocityX
                                offsetY += velocityY
                                velocityX *= 0.92f
                                velocityY *= 0.92f
                                
                                if (abs(velocityX) < 0.1f && abs(velocityY) < 0.1f) {
                                    isAnimating = false
                                } else {
                                    invalidate()
                                    postOnAnimation(this)
                                }
                            }
                        })
                    }
                }
            }
        }
        return true
    }

    fun zoomIn() {
        targetScale = (targetScale * 1.2f).coerceIn(0.5f, 5.0f)
        removeCallbacks(zoomAnimator)
        post(zoomAnimator)
    }

    fun zoomOut() {
        targetScale = (targetScale / 1.2f).coerceIn(0.5f, 5.0f)
        removeCallbacks(zoomAnimator)
        post(zoomAnimator)
    }
}
