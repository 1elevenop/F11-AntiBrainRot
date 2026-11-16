package com.f11.app.data

import android.os.Bundle
import java.time.LocalDate
import java.time.LocalDateTime

/** Snapshot of everything F11 stores. All of it lives only in F11's private app storage. */
data class F11Settings(
    val guardEnabled: Boolean = true,
    val swipeLimit: Int = SettingsKeys.DEFAULT_SWIPE_LIMIT,
    val cooldownMinutes: Int = SettingsKeys.DEFAULT_COOLDOWN_MINUTES,
    val disabledPackages: Set<String> = emptySet(),
    val disclosureAccepted: Boolean = false,
    /** Block Reels/Shorts on the first swipe instead of after a budget. */
    val blockShortFormCompletely: Boolean = false,
    /** Block guarded apps entirely: DMs, stories and long videos included. */
    val maxFocus: Boolean = false,
    /** Show the live swipe counter at the top of Reels/Shorts, even with the guard off. */
    val showCounter: Boolean = true,
    val frictionLanguage: FrictionLanguage = FrictionLanguage.ENGLISH,
    val hindiForm: HindiForm = HindiForm.MASCULINE,
    val swipesToday: Int = 0,
    val shieldsToday: Int = 0,
    val blockedUntil: Long = 0L,
    val history: List<DayStat> = emptyList(),
    val today: Long = LocalDate.now().toEpochDay(),
    val firstDay: Long = today,
    val schedules: List<FocusSchedule> = emptyList(),
) {
    val cooldownMs: Long get() = cooldownMinutes * 60_000L

    /** The focus schedule running at [at], if any. */
    fun activeSchedule(at: LocalDateTime = LocalDateTime.now()): FocusSchedule? = FocusSchedules.activeAt(schedules, at)

    /** Max Focus is on when switched on manually or while a schedule runs. */
    fun maxFocusActive(at: LocalDateTime = LocalDateTime.now()): Boolean = maxFocus || activeSchedule(at) != null

    fun isAppGuarded(packageName: String): Boolean = packageName !in disabledPackages

    /** Max Focus works even with the swipe guard off; per-app switches still apply. */
    fun maxFocusApplies(packageName: String, at: LocalDateTime = LocalDateTime.now()): Boolean =
        maxFocusActive(at) && TargetAppsConfig.isTarget(packageName) && isAppGuarded(packageName)

    val focusStreak: Int get() = History.focusStreak(history, today, firstDay)

    fun lastDays(count: Int): List<DayStat> = History.window(history, today, count)
}

/** Read access to stored values: SharedPreferences in F11, a Bundle in the Shield plugin. */
interface SettingsSource {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun getString(key: String): String?
    fun getStringSet(key: String): Set<String>?
}

/** Storage keys and limits, plus the single way stored values become [F11Settings]. */
object SettingsKeys {
    const val DEFAULT_SWIPE_LIMIT = 20
    const val DEFAULT_COOLDOWN_MINUTES = 15
    val SWIPE_LIMIT_RANGE = 5..100
    val COOLDOWN_RANGE = 5..120

    const val GUARD_ENABLED = "guard_enabled"
    const val SWIPE_LIMIT = "swipe_limit"
    const val COOLDOWN_MINUTES = "cooldown_minutes"
    const val DISABLED_PACKAGES = "disabled_packages"
    const val DISCLOSURE_ACCEPTED = "disclosure_accepted"
    const val BLOCK_SHORT_FORM = "block_short_form_completely"
    const val MAX_FOCUS = "max_focus"
    const val SHOW_COUNTER = "show_counter"
    const val FRICTION_LANGUAGE = "friction_language"
    const val HINDI_FORM = "hindi_form"
    const val STATS_DAY = "stats_day"
    const val SWIPES_TODAY = "swipes_today"
    const val SHIELDS_TODAY = "shields_today"
    const val BLOCKED_UNTIL = "blocked_until"
    const val HISTORY = "history"
    const val FIRST_DAY = "first_day"
    const val SCHEDULES = "focus_schedules"

    fun read(source: SettingsSource, today: Long = LocalDate.now().toEpochDay()): F11Settings {
        val isToday = source.getLong(STATS_DAY, -1L) == today
        val swipesToday = if (isToday) source.getInt(SWIPES_TODAY, 0) else 0
        val shieldsToday = if (isToday) source.getInt(SHIELDS_TODAY, 0) else 0
        // Today's live counters are the source of truth for today's history entry.
        val history = History.decode(source.getString(HISTORY)).let {
            if (swipesToday > 0 || shieldsToday > 0) History.upsert(it, DayStat(today, swipesToday, shieldsToday)) else it
        }
        return F11Settings(
            guardEnabled = source.getBoolean(GUARD_ENABLED, true),
            swipeLimit = source.getInt(SWIPE_LIMIT, DEFAULT_SWIPE_LIMIT).coerceIn(SWIPE_LIMIT_RANGE),
            cooldownMinutes = source.getInt(COOLDOWN_MINUTES, DEFAULT_COOLDOWN_MINUTES).coerceIn(COOLDOWN_RANGE),
            disabledPackages = source.getStringSet(DISABLED_PACKAGES).orEmpty(),
            disclosureAccepted = source.getBoolean(DISCLOSURE_ACCEPTED, false),
            blockShortFormCompletely = source.getBoolean(BLOCK_SHORT_FORM, false),
            maxFocus = source.getBoolean(MAX_FOCUS, false),
            showCounter = source.getBoolean(SHOW_COUNTER, true),
            frictionLanguage = FrictionLanguage.fromCode(source.getString(FRICTION_LANGUAGE)),
            hindiForm = HindiForm.fromCode(source.getString(HINDI_FORM)),
            swipesToday = swipesToday,
            shieldsToday = shieldsToday,
            blockedUntil = source.getLong(BLOCKED_UNTIL, 0L),
            history = history,
            today = today,
            firstDay = source.getLong(FIRST_DAY, today),
            schedules = FocusSchedules.decode(source.getString(SCHEDULES)),
        )
    }
}

/** A [SettingsSource] over a Bundle of raw stored values (as sent to the Shield plugin). */
class BundleSettingsSource(private val bundle: Bundle) : SettingsSource {
    override fun getBoolean(key: String, default: Boolean) = bundle.getBoolean(key, default)
    override fun getInt(key: String, default: Int) = bundle.getInt(key, default)
    override fun getLong(key: String, default: Long) = bundle.getLong(key, default)
    override fun getString(key: String): String? = bundle.getString(key)
    override fun getStringSet(key: String): Set<String>? = bundle.getStringArrayList(key)?.toSet()
}
