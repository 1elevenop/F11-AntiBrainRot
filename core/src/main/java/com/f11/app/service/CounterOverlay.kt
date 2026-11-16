package com.f11.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Small, non-interactive pill at the top centre of Reels/Shorts showing today's swipe count.
 * It never takes touches or focus, so the app underneath works normally.
 */
class CounterOverlay(
    private val service: Context,
    private val windowType: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var pill: TextView? = null

    val isShowing: Boolean get() = pill != null

    fun show(text: String) {
        val view = pill ?: create() ?: return
        if (view.text != text) view.text = text
    }

    fun hide() {
        val view = pill ?: return
        pill = null
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Counter already detached", e)
        }
    }

    private fun create(): TextView? {
        val view = TextView(service).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.04f
            val h = dp(14)
            val v = dp(7)
            setPadding(h, v, h, v)
            background = GradientDrawable().apply {
                cornerRadius = dp(100).toFloat()
                setColor(Color.argb(200, 0, 0, 0))
                setStroke(dp(1), Color.argb(110, 124, 92, 255))
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight() + dp(10)
            title = "F11 Counter"
        }
        return try {
            windowManager.addView(view, params)
            pill = view
            view
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not attach counter", e)
            null
        }
    }

    private fun statusBarHeight(): Int {
        @Suppress("DiscouragedApi")
        val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) service.resources.getDimensionPixelSize(id) else dp(28)
    }

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "F11Counter"
    }
}
