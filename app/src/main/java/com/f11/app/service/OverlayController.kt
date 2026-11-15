package com.f11.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.f11.app.R
import java.util.Locale

/** Why the shield is up. */
sealed interface ShieldMode {
    /** Swipe budget spent; short-form feed locked until [blockedUntil] (wall clock). */
    data class Cooldown(val appName: String, val surfaceName: String, val blockedUntil: Long) : ShieldMode

    /** Short-form feed blocked outright; the rest of the app stays usable. */
    data class ShortFormBlocked(val appName: String, val surfaceName: String) : ShieldMode

    /** Max Focus: the whole app is blocked. */
    data class MaxFocus(val appName: String, val schedule: String? = null) : ShieldMode
}

/**
 * Draws the full-screen F11 shield with TYPE_ACCESSIBILITY_OVERLAY, which an
 * AccessibilityService may use without the SYSTEM_ALERT_WINDOW permission.
 * Must be called on the main thread.
 */
class OverlayController(
    private val service: AccessibilityService,
    /** Called after the shield is dismissed by its button; Max Focus leaves the app, others step back. */
    private val onLeave: (ShieldMode) -> Unit,
    private val onCooldownFinished: () -> Unit,
    /** A fresh strong line each time the shield goes up. */
    private val strongLine: () -> String,
) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private var shield: View? = null
    private var countdownView: TextView? = null

    var mode: ShieldMode? = null
        private set

    val isShowing: Boolean get() = shield != null

    private val ticker = object : Runnable {
        override fun run() {
            val cooldown = mode as? ShieldMode.Cooldown ?: return
            val remaining = cooldown.blockedUntil - System.currentTimeMillis()
            if (remaining <= 0L) {
                hide()
                onCooldownFinished()
                return
            }
            countdownView?.text = service.getString(R.string.overlay_countdown, cooldown.surfaceName, formatRemaining(remaining))
            // Tick on the next whole second so the display never skips.
            handler.postAtTime(this, SystemClock.uptimeMillis() + (remaining % 1_000L).coerceAtLeast(1L))
        }
    }

    fun show(newMode: ShieldMode) {
        if (shield != null && mode == newMode) return
        val wasShowing = shield != null
        val view = shield ?: inflate() ?: return
        if (!wasShowing) {
            view.findViewById<TextView>(R.id.f11_shield_line).text = "“${strongLine()}”"
        }
        mode = newMode
        bind(view, newMode)
        handler.removeCallbacks(ticker)
        if (newMode is ShieldMode.Cooldown) ticker.run()
    }

    fun hide() {
        handler.removeCallbacks(ticker)
        mode = null
        val view = shield ?: return
        shield = null
        countdownView = null
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Shield already detached", e)
        }
    }

    private fun inflate(): View? {
        val themed = ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault)
        val view = LayoutInflater.from(themed).inflate(R.layout.overlay_f11_shield, null)
        view.findViewById<Button>(R.id.f11_shield_leave).setOnClickListener {
            val leaving = mode ?: return@setOnClickListener
            hide()
            onLeave(leaving)
        }
        return try {
            windowManager.addView(view, layoutParams())
            shield = view
            countdownView = view.findViewById(R.id.f11_shield_countdown)
            view
        } catch (e: RuntimeException) {
            // BadTokenException / IllegalStateException if the service is being torn down.
            Log.w(TAG, "Could not attach F11 shield", e)
            null
        }
    }

    private fun bind(view: View, mode: ShieldMode) {
        val headline = view.findViewById<TextView>(R.id.f11_shield_headline)
        val body = view.findViewById<TextView>(R.id.f11_shield_body)
        val countdown = view.findViewById<TextView>(R.id.f11_shield_countdown)
        val leave = view.findViewById<Button>(R.id.f11_shield_leave)
        when (mode) {
            is ShieldMode.Cooldown -> {
                headline.setText(R.string.overlay_headline)
                body.text = service.getString(R.string.overlay_body_cooldown, mode.appName)
                countdown.visibility = View.VISIBLE
                leave.text = service.getString(R.string.overlay_leave_surface, mode.surfaceName)
            }
            is ShieldMode.ShortFormBlocked -> {
                headline.text = service.getString(R.string.overlay_headline_blocked, mode.surfaceName)
                body.text = service.getString(R.string.overlay_body_blocked, mode.appName)
                countdown.visibility = View.GONE
                leave.text = service.getString(R.string.overlay_leave_surface, mode.surfaceName)
            }
            is ShieldMode.MaxFocus -> {
                headline.setText(R.string.overlay_headline_max_focus)
                body.text = if (mode.schedule != null) {
                    service.getString(R.string.overlay_body_max_focus_scheduled, mode.appName, mode.schedule)
                } else {
                    service.getString(R.string.overlay_body_max_focus, mode.appName)
                }
                countdown.visibility = View.GONE
                leave.setText(R.string.overlay_leave_home)
            }
        }
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.CENTER
        title = "F11 Shield"
    }

    private fun formatRemaining(ms: Long): String {
        val totalSeconds = (ms + 999L) / 1_000L
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private companion object {
        const val TAG = "F11Overlay"
    }
}
