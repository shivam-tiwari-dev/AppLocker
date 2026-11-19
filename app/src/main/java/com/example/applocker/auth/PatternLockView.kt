
package com.example.applocker.auth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View

class PatternLockView(context: Context) : View(context) {

    private val selectedDots = mutableListOf<Int>()
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var activeX = 0f
    private var activeY = 0f
    private var drawing = false

    var onPatternComplete: ((String) -> Unit)? = null

    init {
        dotPaint.color = 0xFF303F9F.toInt()
        dotPaint.style = Paint.Style.FILL

        linePaint.color = 0xFF303F9F.toInt()
        linePaint.strokeWidth = 8f
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
    }

    private fun getDotPosition(index: Int): Pair<Float, Float> {
        val spacingX = width / 4f
        val spacingY = height / 4f

        val row = index / 3
        val col = index % 3

        return Pair(
            spacingX * (col + 1),
            spacingY * (row + 1)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val path = Path()

        selectedDots.forEachIndexed { index, dot ->
            val (x, y) = getDotPosition(dot)

            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        if (drawing && selectedDots.isNotEmpty()) {
            path.lineTo(activeX, activeY)
        }

        canvas.drawPath(path, linePaint)

        for (i in 0..8) {
            val (x, y) = getDotPosition(i)

            dotPaint.color = if (i in selectedDots) {
                0xFF16A34A.toInt()
            } else {
                0xFF303F9F.toInt()
            }

            canvas.drawCircle(x, y, 18f, dotPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedDots.clear()
                drawing = true
                addDotAt(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                activeX = event.x
                activeY = event.y
                addDotAt(event.x, event.y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                drawing = false
                invalidate()

                val result = selectedDots.joinToString("-") { (it + 1).toString() }
                onPatternComplete?.invoke(result)

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                drawing = false
                selectedDots.clear()
                invalidate()
                return true
            }
        }

        return true
    }

    private fun addDotAt(x: Float, y: Float) {
        for (i in 0..8) {
            val (dotX, dotY) = getDotPosition(i)

            val distance = kotlin.math.hypot(
                x - dotX,
                y - dotY
            )

            if (distance < 55f && i !in selectedDots) {
                selectedDots.add(i)
                break
            }
        }
    }

    fun clearPattern() {
        selectedDots.clear()
        invalidate()
    }
}
