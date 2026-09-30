package com.example.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.model.CursorMode

class FloatingQuickDockView(
    context: Context,
    private val onModeChanged: (CursorMode) -> Unit,
    private val onBackClicked: () -> Unit,
    private val onHomeClicked: () -> Unit,
    private val onRecentsClicked: () -> Unit,
    private val onCalibrateClicked: () -> Unit,
    private val onPauseToggled: () -> Unit,
    private val onDismissRequest: () -> Unit
) : LinearLayout(context) {

    private var isExpanded = true
    private var isPaused = false
    private var currentMode = CursorMode.CLICK

    private val actionsContainer: LinearLayout
    private val pauseButton: TextView
    private val modeButton: TextView
    private val minimizeButton: TextView
    private val statusIndicator: View

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = (8 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad / 2, pad, pad / 2)

        // Sleek cyber pill background
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 40f * resources.displayMetrics.density
            setColor(0xE6101726.toInt()) // dark translucent
            setStroke((1.5f * resources.displayMetrics.density).toInt(), 0xFF00E5FF.toInt())
        }
        background = bgDrawable

        // Status indicator dot (green = active, yellow = paused)
        statusIndicator = View(context).apply {
            val dotSize = (10 * resources.displayMetrics.density).toInt()
            layoutParams = LayoutParams(dotSize, dotSize).apply {
                marginEnd = (6 * resources.displayMetrics.density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF00E676.toInt())
            }
        }
        addView(statusIndicator)

        // Actions container
        actionsContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Mode switch button
        modeButton = createActionButton("🎯 Клік") {
            currentMode = when (currentMode) {
                CursorMode.CLICK -> CursorMode.SCROLL_DOWN
                CursorMode.SCROLL_DOWN -> CursorMode.SCROLL_UP
                CursorMode.SCROLL_UP -> CursorMode.LONG_PRESS
                CursorMode.LONG_PRESS -> CursorMode.CLICK
            }
            updateModeDisplay()
            onModeChanged(currentMode)
        }
        actionsContainer.addView(modeButton)

        // Back button
        val backBtn = createActionButton("◀ Назад") {
            onBackClicked()
        }
        actionsContainer.addView(backBtn)

        // Home button
        val homeBtn = createActionButton("⌂ Дім") {
            onHomeClicked()
        }
        actionsContainer.addView(homeBtn)

        // Recents button
        val recentsBtn = createActionButton("▦ Меню") {
            onRecentsClicked()
        }
        actionsContainer.addView(recentsBtn)

        // Calibrate button
        val calibBtn = createActionButton("⚙ Центр") {
            onCalibrateClicked()
        }
        actionsContainer.addView(calibBtn)

        // Pause/Resume button
        pauseButton = createActionButton("⏸") {
            isPaused = !isPaused
            updatePauseDisplay()
            onPauseToggled()
        }
        actionsContainer.addView(pauseButton)

        addView(actionsContainer)

        // Minimize toggle
        minimizeButton = createActionButton("✕") {
            isExpanded = !isExpanded
            actionsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            minimizeButton.text = if (isExpanded) "✕" else "👁 EyeCursor"
        }
        addView(minimizeButton)
    }

    private fun createActionButton(label: String, onClick: () -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * dp
                setColor(0x33FFFFFF)
            }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = (3 * dp).toInt()
                marginEnd = (3 * dp).toInt()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun updateModeDisplay() {
        modeButton.text = when (currentMode) {
            CursorMode.CLICK -> "🎯 Клік"
            CursorMode.SCROLL_DOWN -> "⬇ Скрол Вниз"
            CursorMode.SCROLL_UP -> "⬆ Скрол Вгору"
            CursorMode.LONG_PRESS -> "⏱ Довгий клік"
        }
    }

    private fun updatePauseDisplay() {
        pauseButton.text = if (isPaused) "▶ Пуск" else "⏸ Пауза"
        val dotBg = statusIndicator.background as? GradientDrawable
        dotBg?.setColor(if (isPaused) 0xFFFFB300.toInt() else 0xFF00E676.toInt())
    }

    fun setTrackingActive(active: Boolean) {
        val dotBg = statusIndicator.background as? GradientDrawable
        dotBg?.setColor(if (active) 0xFF00E676.toInt() else 0xFFFF5252.toInt())
    }
}
