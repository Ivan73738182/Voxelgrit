package com.ivangames.voxelgrit

import android.annotation.SuppressLint
import android.content.Context
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

    // Кисти
    private val waterPaint = Paint().apply { color = Color.parseColor("#1A237E") }
    private val landPaint = Paint().apply { color = Color.parseColor("#D2B48C") }
    // Убрали gridPaint (сетку)
    private val dotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pixelSize = 20f
    private val mapGrid = arrayOf(
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000011100000000000000000000000000000000",
        "0001111110000000000000000000001111000000",
        "0011111111100000000000000000111111110000",
        "0001111111111000000000000001111111111000",
        "0000111111110000000000000001111111110000",
        "0000011111110000000000000000111111100000",
        "0000001111100000000000000000011111000000",
        "0000000110000000000000000000001110000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000011000000000000000000000",
        "0000000000000001111000000000000000000000",
        "0000000000000011111000000000000000000000",
        "0000000000000011110000000000000000000000",
        "0000000000000001100000000000000000000000",
        "0000000000000001000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000"
    )

    init {
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)
                targetScale = scaleFactor // Синхронизируем цель
                invalidate()
                return true
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#1A237E"))

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        canvas.translate(offsetX, offsetY)

        val mapWidth = mapGrid[0].length * pixelSize
        val mapHeight = mapGrid.size * pixelSize
        val startX = (width - mapWidth) / 2f
        val startY = (height - mapHeight) / 2f

        // Рисуем материки без сетки
        for (y in mapGrid.indices) {
            for (x in mapGrid[y].indices) {
                if (mapGrid[y][x] == '1') {
                    val left = startX + x * pixelSize
                    val top = startY + y * pixelSize
                    canvas.drawRect(left, top, left + pixelSize, top + pixelSize, landPaint)
                }
            }
        }

        // Рисуем точки
        for (y in mapGrid.indices) {
            for (x in mapGrid[y].indices) {
                if (mapGrid[y][x] == '1' && (x + y) % 2 == 0) {
                    val cx = startX + x * pixelSize + pixelSize / 2f
                    val cy = startY + y * pixelSize + pixelSize / 2f
                    canvas.drawCircle(cx, cy, 3f / scaleFactor, dotPaint)
                }
            }
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
