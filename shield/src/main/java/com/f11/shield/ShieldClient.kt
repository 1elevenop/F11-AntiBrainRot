package com.f11.shield

import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.f11.app.data.BundleSettingsSource
import com.f11.app.data.F11Settings
import com.f11.app.data.SettingsKeys
import com.f11.app.data.ShieldContract

/**
 * The plugin's view of F11's data. F11 owns the settings and counters; this caches the latest
 * copy and sends swipes and shields back through F11's ContentProvider. [current] is null while
 * F11 isn't installed (or the call is refused), and the shield then stays idle.
 */
class ShieldClient(context: Context) {

    private val resolver = context.applicationContext.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private var lastAttempt = 0L
    private var observing = false

    var current: F11Settings? = null
        private set

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    /** Fetches fresh settings now. Safe to call often; failures are retried at most every few seconds. */
    fun refresh(): F11Settings? = send(ShieldContract.METHOD_GET_SETTINGS)

    /** Returns cached settings, re-trying the connection to F11 now and then while it's missing. */
    fun settings(): F11Settings? {
        if (current == null && SystemClock.elapsedRealtime() - lastAttempt > RETRY_MS) refresh()
        return current
    }

    fun recordSwipe() {
        send(ShieldContract.METHOD_RECORD_SWIPE)
    }

    fun recordShield(blockedUntil: Long? = null) {
        val extras = blockedUntil?.let { Bundle().apply { putLong(ShieldContract.EXTRA_BLOCKED_UNTIL, it) } }
        send(ShieldContract.METHOD_RECORD_SHIELD, extras)
    }

    fun clearBlock() {
        send(ShieldContract.METHOD_CLEAR_BLOCK)
    }

    fun close() {
        if (observing) resolver.unregisterContentObserver(observer)
        observing = false
    }

    private fun send(method: String, extras: Bundle? = null): F11Settings? {
        lastAttempt = SystemClock.elapsedRealtime()
        current = try {
            resolver.call(ShieldContract.SETTINGS_URI, method, null, extras)
                ?.let { SettingsKeys.read(BundleSettingsSource(it)) }
        } catch (e: IllegalArgumentException) {
            null // F11 isn't installed.
        } catch (e: SecurityException) {
            Log.w(TAG, "F11 refused the Shield plugin; are both signed with the same key?", e)
            null
        }
        if (current != null && !observing) observe()
        return current
    }

    private fun observe() {
        observing = try {
            resolver.registerContentObserver(ShieldContract.SETTINGS_URI, false, observer)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Can't watch F11 settings", e)
            false
        }
    }

    private companion object {
        const val TAG = "F11ShieldClient"
        const val RETRY_MS = 5_000L
    }
}
