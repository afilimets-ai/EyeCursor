package com.example.ui.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator

class CursorOverlayView(context: Context) : View(context) {

    private var cursorX = 300f
    private var cursorY = 500f
    private var dwellProgress = 0f
    private var blinkProgress = 0f
    private var gestureLabel = ""
    private var cursorSizePx = 72f
    private var cursorColor = 0xFF00E5FF.toInt()

    private var rippleRadius = 0f
    private var rippleAlpha = 0
    private var rippleAnimator: ValueAnimator? = null

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
        alpha = 180
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
    }

    private val blinkChargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7f
        color = 0xFFFFB300.toInt() // Amber charge
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
        alpha = 140
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val progressRect = RectF()
    private val blinkRect = RectF()

    fun updatePosition(x: Float, y: Float, dwell: Float, blink: Float, gesture: String = "") {
        cursorX = x
        cursorY = y
        dwellProgress = dwell
        blinkProgress = blink
        gestureLabel = gesture
        invalidate()
    }

    fun setCursorConfig(sizeDp: Int, colorHex: Long) {
        val density = resources.displayMetrics.density
        cursorSizePx = sizeDp * density
        cursorColor = colorHex.toInt()
        invalidate()
    }

    fun triggerClickAnimation() {
        rippleAnimator?.cancel()
        rippleAnimator = ValueAnimator.ofFloat(cursorSizePx * 0.4f, cursorSizePx * 2.4f).apply {
            duration = 380
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                rippleRadius = animator.animatedValue as Float
                val fraction = animator.animatedFraction
                rippleAlpha = ((1f - fraction) * 255).toInt()
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = cursorSizePx / 2f
        progressPaint.color = cursorColor
        centerPaint.color = cursorColor
        ripplePaint.color = if (blinkProgress > 0f) 0xFFFFB300.toInt() else cursorColor

        // Draw click ripple if active
        if (rippleAlpha > 0) {
            ripplePaint.alpha = rippleAlpha
            canvas.drawCircle(cursorX, cursorY, rippleRadius, ripplePaint)
        }

        // Draw crosshair ticks
        val tickOffset = radius * 0.35f
        val tickLength = radius * 0.65f
        canvas.drawLine(cursorX - tickLength, cursorY, cursorX - tickOffset, cursorY, crosshairPaint)
        canvas.drawLine(cursorX + tickOffset, cursorY, cursorX + tickLength, cursorY, crosshairPaint)
        canvas.drawLine(cursorX, cursorY - tickLength, cursorX, cursorY - tickOffset, crosshairPaint)
        canvas.drawLine(cursorX, cursorY + tickOffset, cursorX, cursorY + tickLength, crosshairPaint)

        // Draw outer ring
        canvas.drawCircle(cursorX, cursorY, radius, outerPaint)

        // Draw dwell progress arc
        if (dwellProgress > 0f) {
            val sweepAngle = dwellProgress * 360f
            progressRect.set(
                cursorX - radius,
                cursorY - radius,
                cursorX + radius,
                cursorY + radius
            )
            canvas.drawArc(progressRect, -90f, sweepAngle, false, progressPaint)
        }

        // Draw sustained blink / wink charging progress arc
        if (blinkProgress > 0f) {
            val blinkRadius = radius * 0.72f
            blinkRect.set(
                cursorX - blinkRadius,
                cursorY - blinkRadius,
                cursorX + blinkRadius,
                cursorY + blinkRadius
            )
            val sweepAngle = blinkProgress * 360f
            canvas.drawArc(blinkRect, -90f, sweepAngle, false, blinkChargePaint)

            // Inner pulsing dot
            canvas.drawCircle(cursorX, cursorY, radius * (0.25f + blinkProgress * 0.25f), blinkChargePaint)

            if (gestureLabel.isNotEmpty()) {
                canvas.drawText(gestureLabel, cursorX, cursorY + radius + 32f, textPaint)
            }
        } else {
            // Center reticle dot
            canvas.drawCircle(cursorX, cursorY, radius * 0.22f, centerPaint)
        }
    }
}
