package com.f11.app.focus

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.f11.app.MainActivity
import com.f11.app.R
import com.f11.app.data.F11Settings
import com.f11.app.data.FocusSchedule
import com.f11.app.data.FrictionLines
import com.f11.app.data.PreferencesManager
import com.f11.app.data.ShieldStatus
import com.f11.app.data.TargetAppsConfig
import com.f11.app.service.OverlayController
import com.f11.app.service.ShieldMode

/**
 * Max Focus and focus schedules without the Accessibility API. Android's usage stats say which
 * app is in front; a guarded app gets covered by the F11 shield ("Display over other apps").
 * Only package names are used, on-device. Stands down while the F11 Shield plugin is on, since
 * the plugin enforces Max Focus itself.
 */
class FocusWatchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { PreferencesManager.get(this) }
    private val usage by lazy { getSystemService(UsageStatsManager::class.java) }
    private var overlay: OverlayController? = null
    private var foreground: String? = null
    private var lastQuery = 0L
    private var shownText: String? = null

    private val tick = object : Runnable {
        override fun run() {
            val delay = check()
            handler.postDelayed(this, delay)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlay = OverlayController(
            service = this,
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            onLeave = { goHome() },
            onCooldownFinished = {},
            strongLine = {
                val s = prefs.current
                FrictionLines.pick(1, s.frictionLanguage, s.hindiForm).first()
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(prefs.current)
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    /** One pass; returns how long to wait before the next. */
    private fun check(): Long {
        prefs.refresh() // a new day, or a schedule edited elsewhere
        val settings = prefs.current
        if (!needsWatch(settings) || !canWatch(this)) {
            stop()
            return IDLE_MS
        }
        startInForeground(settings)
        if (ShieldStatus.isRunning(this)) {
            overlay?.hide() // the plugin handles Max Focus
            return IDLE_MS
        }
        if (!settings.maxFocusActive()) {
            overlay?.hide()
            lastQuery = 0L
            return IDLE_MS
        }
        val front = updateForeground() ?: return ACTIVE_MS
        if (settings.maxFocusApplies(front)) raise(front, settings) else overlay?.hide()
        return ACTIVE_MS
    }

    /** The most recently resumed app, from usage events since the last look. */
    @Suppress("DEPRECATION") // MOVE_TO_FOREGROUND == ACTIVITY_RESUMED, and works back to API 26.
    private fun updateForeground(): String? {
        val now = System.currentTimeMillis()
        val from = if (lastQuery == 0L) now - LOOKBACK_MS else lastQuery
        lastQuery = now
        val events = usage?.queryEvents(from, now) ?: return foreground
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                val pkg = event.packageName
                if (pkg != packageName && pkg !in IGNORED) foreground = pkg
            }
        }
        return foreground
    }

    private fun raise(packageName: String, settings: F11Settings) {
        val app = TargetAppsConfig.find(packageName) ?: return
        val schedule = if (settings.maxFocus) null else settings.activeSchedule()
        val mode = ShieldMode.MaxFocus(
            app.displayName,
            schedule?.let { "${it.name} · until ${FocusSchedule.formatMinute(it.endMinute)}" },
        )
        if (overlay?.mode != mode) {
            prefs.recordShield()
            overlay?.show(mode)
        }
    }

    private fun goHome() {
        foreground = null
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun startInForeground(settings: F11Settings) {
        val text = when {
            settings.maxFocus -> "Max Focus is on. Guarded apps are blocked."
            settings.activeSchedule() != null -> "${settings.activeSchedule()?.name} is running."
            else -> "Focus schedule armed."
        }
        if (text == shownText) return
        shownText = text
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Focus watch", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("F11")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stop() {
        shownText = null
        overlay?.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        overlay?.hide()
        overlay = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "focus_watch"
        private const val NOTIFICATION_ID = 11
        private const val ACTIVE_MS = 1_000L
        private const val IDLE_MS = 15_000L
        private const val LOOKBACK_MS = 60_000L
        private val IGNORED = setOf("com.android.systemui", "android")

        /** Watching is needed while Max Focus is on or any schedule could start. */
        fun needsWatch(settings: F11Settings): Boolean =
            settings.maxFocus || settings.schedules.any { it.enabled }

        fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
            @Suppress("DEPRECATION")
            val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun canDrawOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

        fun canWatch(context: Context): Boolean = hasUsageAccess(context) && canDrawOverlay(context)

        /** Starts or stops the watcher to match [settings]. Call from the foreground or at boot. */
        fun sync(context: Context, settings: F11Settings) {
            val intent = Intent(context, FocusWatchService::class.java)
            if (needsWatch(settings) && canWatch(context)) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.stopService(intent)
            }
        }
    }
}
