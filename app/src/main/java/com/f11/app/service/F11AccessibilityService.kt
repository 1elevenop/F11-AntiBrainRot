package com.f11.app.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.f11.app.data.F11Settings
import com.f11.app.data.FocusSchedule
import com.f11.app.data.FrictionLines
import com.f11.app.data.PreferencesManager
import com.f11.app.data.TargetApp
import com.f11.app.data.TargetAppsConfig

/**
 * Watches for swipes in short-form video feeds and raises the shield when the user's
 * swipe budget runs out (or on the first swipe, if they chose to block those feeds outright).
 * In Max Focus it blocks guarded apps as soon as they come to the front. Only package names, view resource ids and class names are inspected;
 * no on-screen text is read and nothing is stored beyond counters in private preferences.
 */
class F11AccessibilityService : AccessibilityService() {

    private lateinit var prefs: PreferencesManager
    private lateinit var tracker: ScrollTracker
    private var overlay: OverlayController? = null
    private var counter: CounterOverlay? = null
    private val awarenessDebouncer = SwipeDebouncer()
    private var foregroundPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pagerApp: TargetApp? = null
    private var lastPageKey: Int? = null

    /** Watches feeds that never send scroll events (YouTube Shorts) while they are in front. */
    private val pagerPoll = object : Runnable {
        override fun run() {
            val app = pagerApp ?: return
            pollPager(app)
            handler.postDelayed(this, PAGER_POLL_MS)
        }
    }

    /** Starts or ends scheduled Max Focus on time, even if the user is sitting in a guarded app. */
    private val scheduleTick = object : Runnable {
        override fun run() {
            if (::prefs.isInitialized) {
                val settings = prefs.current
                syncWithSettings(settings)
                // Ask Android what's really in front; never shield F11 itself or a non-target app.
                val front = rootInActiveWindow?.packageName?.toString()
                if (overlay?.isShowing != true && front != null && maxFocusApplies(front, settings)) {
                    raiseMaxFocus(front, settings)
                }
            }
            handler.postDelayed(this, SCHEDULE_TICK_MS)
        }
    }

    /** Logs only view ids and package names, and only in debuggable builds. */
    private val isDebugBuild: Boolean by lazy {
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager.get(this)
        val settings = prefs.current
        tracker = ScrollTracker(
            swipeLimit = settings.swipeLimit,
            cooldownMs = settings.cooldownMs,
            initialBlockedUntil = settings.blockedUntil,
        )
        overlay = OverlayController(
            service = this,
            onLeave = ::leave,
            onCooldownFinished = { prefs.clearBlock() },
            strongLine = {
                val s = prefs.current
                FrictionLines.pick(1, s.frictionLanguage, s.hindiForm).first()
            },
        )
        counter = CounterOverlay(this)
        handler.postDelayed(scheduleTick, SCHEDULE_TICK_MS)
        // F11 may (re)start while a guarded app is already in front.
        rootInActiveWindow?.packageName?.toString()?.let { onWindowChanged(it, prefs.current) }
    }

    /**
     * Max Focus leaves the app. Otherwise step back out of Reels/Shorts so DMs, stories and
     * regular videos stay reachable; if the feed is a main tab that Back can't leave (YouTube
     * Shorts), fall back to the home screen.
     */
    private fun leave(mode: ShieldMode) {
        if (mode is ShieldMode.MaxFocus) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }
        val app = TargetAppsConfig.find(foregroundPackage)
        // Wait for the shield window to detach, or it swallows the Back press.
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (app?.feedViewId != null) {
                handler.postDelayed({ if (isFeedOnScreen(app)) performGlobalAction(GLOBAL_ACTION_HOME) }, LEAVE_CHECK_MS)
            }
        }, SHIELD_DETACH_MS)
    }

    private fun findFeed(app: TargetApp): AccessibilityNodeInfo? {
        val id = app.feedViewId ?: return null
        val root = rootInActiveWindow ?: return null
        if (root.packageName?.toString() != app.packageName) return null
        return root.findAccessibilityNodeInfosByViewId(id).firstOrNull { it.isVisibleToUser }
    }

    private fun isFeedOnScreen(app: TargetApp): Boolean = findFeed(app) != null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::tracker.isInitialized) return
        val packageName = event.packageName?.toString() ?: return
        val settings = prefs.current
        syncWithSettings(settings)

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // F11's own overlays also raise window events, so only F11's real screen counts.
                if (packageName == this.packageName && event.className?.toString() == MAIN_ACTIVITY) {
                    counter?.hide()
                }
                onWindowChanged(packageName, settings)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> onScrolled(event, packageName, settings)
        }
    }

    private fun onWindowChanged(packageName: String, settings: F11Settings) {
        if (packageName in IGNORED_WINDOW_PACKAGES || packageName == this.packageName) return
        val previous = foregroundPackage
        foregroundPackage = packageName
        if (packageName != previous) counter?.hide()
        updatePagerWatch(packageName, settings)
        when {
            maxFocusApplies(packageName, settings) -> raiseMaxFocus(packageName, settings)
            // A feed shield stays up while the same app is in front; it only goes when you leave.
            packageName != previous || overlay?.mode is ShieldMode.MaxFocus -> overlay?.hide()
        }
    }

    private fun onScrolled(event: AccessibilityEvent, packageName: String, settings: F11Settings) {
        if (maxFocusApplies(packageName, settings)) {
            raiseMaxFocus(packageName, settings)
            return
        }
        // Swipes are counted in every target app, guard or not; enforcement is separate.
        val app = TargetAppsConfig.find(packageName) ?: return
        val viewIds = collectViewIds(event)
        val shortForm = TargetAppsConfig.isShortFormSurface(packageName, viewIds)
        if (isDebugBuild) Log.d(TAG, "scroll pkg=$packageName shortForm=$shortForm ids=$viewIds")
        if (shortForm) onShortFormSwipe(app, settings) else counter?.hide()
    }

    private fun updatePagerWatch(packageName: String, settings: F11Settings) {
        val app = TargetAppsConfig.find(packageName)?.takeIf { it.pollFeed }
        if (app == pagerApp) return
        handler.removeCallbacks(pagerPoll)
        pagerApp = app
        lastPageKey = null
        if (app != null) handler.post(pagerPoll)
    }

    /**
     * Finds the paging list and identifies the page filling the screen by its node identity
     * (no text is read). A new identity means the user swiped to another Short.
     */
    private fun pollPager(app: TargetApp) {
        val settings = prefs.current
        if (maxFocusApplies(app.packageName, settings)) return
        if (overlay?.isShowing == true) return
        val pager = findFeed(app)
        if (pager == null) {
            lastPageKey = null
            counter?.hide()
            return
        }
        val pagerBounds = Rect().also(pager::getBoundsInScreen)
        val page = (0 until pager.childCount).asSequence()
            .mapNotNull(pager::getChild)
            .maxByOrNull { child ->
                val r = Rect().also(child::getBoundsInScreen)
                if (r.intersect(pagerBounds)) r.width().toLong() * r.height() else 0L
            } ?: return
        val key = page.hashCode()
        val previous = lastPageKey
        lastPageKey = key
        if (isDebugBuild && previous != key) Log.d(TAG, "pager ${app.packageName} page=$key previous=$previous")
        val enforced = isGuarded(app.packageName, settings)
        when {
            enforced && settings.blockShortFormCompletely -> onShortFormSwipe(app, settings)
            previous != null && previous != key -> onShortFormSwipe(app, settings)
            else -> updateCounter(settings, enforced)
        }
    }

    private fun onShortFormSwipe(app: TargetApp, settings: F11Settings) {
        val enforced = isGuarded(app.packageName, settings)
        if (enforced && settings.blockShortFormCompletely) {
            if (overlay?.mode !is ShieldMode.ShortFormBlocked) {
                prefs.recordShield()
                overlay?.show(ShieldMode.ShortFormBlocked(app.displayName, app.surfaceName))
            }
            counter?.hide()
            return
        }

        val now = System.currentTimeMillis()
        if (enforced) {
            val result = tracker.onScrollEvent(now)
            if (isDebugBuild && result != ScrollTracker.Result.Debounced) Log.d(TAG, "tracker: $result")
            when (result) {
                is ScrollTracker.Result.Counted -> prefs.recordSwipe()
                is ScrollTracker.Result.LimitReached -> {
                    prefs.recordSwipe()
                    prefs.recordShield(result.blockedUntil)
                    overlay?.show(ShieldMode.Cooldown(app.displayName, app.surfaceName, result.blockedUntil))
                }
                is ScrollTracker.Result.Blocked ->
                    overlay?.show(ShieldMode.Cooldown(app.displayName, app.surfaceName, result.blockedUntil))
                ScrollTracker.Result.Debounced -> Unit
            }
        } else if (awarenessDebouncer.isNewSwipe(now)) {
            prefs.recordSwipe()
            if (isDebugBuild) Log.d(TAG, "awareness swipe (guard off)")
        }
        updateCounter(prefs.current, enforced)
    }

    /** Live pill at the top of the feed: today's swipes, plus what's left when enforcing. */
    private fun updateCounter(settings: F11Settings, enforced: Boolean) {
        if (!settings.showCounter || overlay?.isShowing == true) {
            counter?.hide()
            return
        }
        val today = settings.swipesToday
        val text = if (enforced && !settings.blockShortFormCompletely) {
            val left = (tracker.swipeLimit - tracker.swipeCount).coerceAtLeast(0)
            "$today today  ·  $left left"
        } else {
            "$today swipes today"
        }
        counter?.show(text)
    }

    private fun raiseMaxFocus(packageName: String, settings: F11Settings) {
        val app = TargetAppsConfig.find(packageName) ?: return
        // A manual Max Focus has no end; a scheduled one shows when it lifts.
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

    private fun isGuarded(packageName: String, settings: F11Settings): Boolean =
        settings.guardEnabled && TargetAppsConfig.isTarget(packageName) && settings.isAppGuarded(packageName)

    /** Max Focus works even with the swipe guard off; per-app switches still apply. */
    private fun maxFocusApplies(packageName: String, settings: F11Settings): Boolean =
        settings.maxFocusActive() && TargetAppsConfig.isTarget(packageName) && settings.isAppGuarded(packageName)

    /** Keeps the tracker and shield in step with settings changed from the dashboard. */
    private fun syncWithSettings(settings: F11Settings) {
        if (tracker.swipeLimit != settings.swipeLimit) tracker.swipeLimit = settings.swipeLimit
        if (tracker.cooldownMs != settings.cooldownMs) tracker.cooldownMs = settings.cooldownMs
        if (!settings.guardEnabled && (tracker.blockedUntil != 0L || tracker.swipeCount != 0)) {
            tracker.reset()
            if (settings.blockedUntil != 0L) prefs.clearBlock()
        }
        val stale = when (overlay?.mode) {
            is ShieldMode.MaxFocus -> !settings.maxFocusActive()
            is ShieldMode.ShortFormBlocked -> !settings.guardEnabled || !settings.blockShortFormCompletely
            is ShieldMode.Cooldown -> !settings.guardEnabled
            null -> false
        }
        val foreground = foregroundPackage
        val foregroundUnguarded = foreground != null &&
            !isGuarded(foreground, settings) && !maxFocusApplies(foreground, settings)
        if (stale || (overlay?.isShowing == true && foregroundUnguarded)) overlay?.hide()
        if (!settings.showCounter || overlay?.isShowing == true) counter?.hide()
    }

    /** Resource ids / class names of the scrolled node and a few ancestors. */
    private fun collectViewIds(event: AccessibilityEvent): List<String?> {
        val ids = mutableListOf<String?>(event.className?.toString())
        var node: AccessibilityNodeInfo? = event.source
        var depth = 0
        while (node != null && depth < MAX_ANCESTOR_DEPTH) {
            ids += node.viewIdResourceName
            ids += node.className?.toString()
            node = node.parent
            depth++
        }
        return ids
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(pagerPoll)
        handler.removeCallbacks(scheduleTick)
        counter?.hide()
        counter = null
        overlay?.hide()
        overlay = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "F11Service"
        const val MAX_ANCESTOR_DEPTH = 4
        val MAIN_ACTIVITY: String = com.f11.app.MainActivity::class.java.name
        const val PAGER_POLL_MS = 1_000L
        const val SCHEDULE_TICK_MS = 30_000L
        const val LEAVE_CHECK_MS = 1_000L
        const val SHIELD_DETACH_MS = 250L

        /** Transient system windows that should not hide the shield. */
        val IGNORED_WINDOW_PACKAGES = setOf("com.android.systemui", "android")
    }
}
