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

class MapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var scaleFactor = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var scaleDetector: ScaleGestureDetector

    // Кисти для рисования
    private val waterPaint = Paint().apply { color = Color.parseColor("#1A237E") } // Тёмно-синий океан
    private val landPaint = Paint().apply { color = Color.parseColor("#D2B48C") }  // Песочный цвет земли
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#000000")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        alpha = 50
    }
    private val dotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Размер одного "пикселя" карты (чем больше, тем крупнее пиксель-арт)
    private val pixelSize = 20f 
    
    // Сетка карты (1 = земля, 0 = вода)
    // Я вручную набросал упрощённые контуры материков (Европа, Африка, Америка и т.д.)
    // Ширина сетки: 40 клеток, Высота: 25 клеток
    private val mapGrid = arrayOf(
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000011100000000000000000000000000000000", // Северная Америка
        "0001111110000000000000000000001111000000",
        "0011111111100000000000000000111111110000",
        "0001111111111000000000000001111111111000",
        "0000111111110000000000000001111111110000",
        "0000011111110000000000000000111111100000",
        "0000001111100000000000000000011111000000",
        "0000000110000000000000000000001110000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000", // Южная Америка
        "0000000000000000011000000000000000000000",
        "0000000000000001111000000000000000000000",
        "0000000000000011111000000000000000000000",
        "0000000000000011110000000000000000000000",
        "0000000000000001100000000000000000000000",
        "0000000000000001000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000", // Африка
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
                invalidate()
                return true
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Заливаем всё океаном
        canvas.drawColor(Color.parseColor("#1A237E"))

        canvas.save()
        // 2. Применяем зум и смещение
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        canvas.translate(offsetX, offsetY)

        // Центрируем карту
        val mapWidth = mapGrid[0].length * pixelSize
        val mapHeight = mapGrid.size * pixelSize
        val startX = (width - mapWidth) / 2f
        val startY = (height - mapHeight) / 2f

        // 3. Рисуем материки
        for (y in mapGrid.indices) {
            for (x in mapGrid[y].indices) {
                if (mapGrid[y][x] == '1') {
                    val left = startX + x * pixelSize
                    val top = startY + y * pixelSize
                    canvas.drawRect(left, top, left + pixelSize, top + pixelSize, landPaint)
                    
                    // Рисуем сетку (как на скрине)
                    canvas.drawRect(left, top, left + pixelSize, top + pixelSize, gridPaint)
                }
            }
        }

        // 4. Рисуем точки (города/провинции) поверх суши
        for (y in mapGrid.indices) {
            for (x in mapGrid[y].indices) {
                if (mapGrid[y][x] == '1') {
                    val cx = startX + x * pixelSize + pixelSize / 2f
                    val cy = startY + y * pixelSize + pixelSize / 2f
                    // Рисуем точку не в каждой клетке, а через одну (для красоты)
                    if ((x + y) % 2 == 0) {
                        canvas.drawCircle(cx, cy, 3f / scaleFactor, dotPaint)
                    }
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
