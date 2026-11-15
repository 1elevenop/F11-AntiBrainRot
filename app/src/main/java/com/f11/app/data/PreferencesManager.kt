package com.f11.app.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Snapshot of everything F11 stores. All of it lives only in private app storage. */
data class F11Settings(
    val guardEnabled: Boolean = true,
    val swipeLimit: Int = PreferencesManager.DEFAULT_SWIPE_LIMIT,
    val cooldownMinutes: Int = PreferencesManager.DEFAULT_COOLDOWN_MINUTES,
    val disabledPackages: Set<String> = emptySet(),
    val disclosureAccepted: Boolean = false,
    /** Block Reels/Shorts on the first swipe instead of after a budget. */
    val blockShortFormCompletely: Boolean = false,
    /** Block guarded apps entirely: DMs, stories and long videos included. */
    val maxFocus: Boolean = false,
    /** Show the live swipe counter at the top of Reels/Shorts, even with the guard off. */
    val showCounter: Boolean = true,
    /** Extra friction layer: say a strong line out loud (on-device recognition). */
    val voiceFriction: Boolean = false,
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

    val focusStreak: Int get() = History.focusStreak(history, today, firstDay)

    fun lastDays(count: Int): List<DayStat> = History.window(history, today, count)
}

/**
 * SharedPreferences-backed store shared by the dashboard and the accessibility service
 * (same process). Changes from either side are pushed to [settings].
 */
class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_FIRST_DAY)) {
            prefs.edit().putLong(KEY_FIRST_DAY, LocalDate.now().toEpochDay()).apply()
        }
    }

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<F11Settings> = _settings.asStateFlow()

    // Held in a field: SharedPreferences only keeps a weak reference to listeners.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = read()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    val current: F11Settings get() = _settings.value

    fun setGuardEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_GUARD_ENABLED, enabled).apply()

    fun setSwipeLimit(limit: Int) =
        prefs.edit().putInt(KEY_SWIPE_LIMIT, limit.coerceIn(SWIPE_LIMIT_RANGE)).apply()

    fun setCooldownMinutes(minutes: Int) =
        prefs.edit().putInt(KEY_COOLDOWN_MINUTES, minutes.coerceIn(COOLDOWN_RANGE)).apply()

    fun setAppGuarded(packageName: String, guarded: Boolean) {
        val updated = current.disabledPackages.toMutableSet().apply {
            if (guarded) remove(packageName) else add(packageName)
        }
        prefs.edit().putStringSet(KEY_DISABLED_PACKAGES, updated).apply()
    }

    fun setBlockShortFormCompletely(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_BLOCK_SHORT_FORM, enabled).apply()

    fun setMaxFocus(enabled: Boolean) = prefs.edit().putBoolean(KEY_MAX_FOCUS, enabled).apply()

    fun setShowCounter(enabled: Boolean) = prefs.edit().putBoolean(KEY_SHOW_COUNTER, enabled).apply()

    fun setVoiceFriction(enabled: Boolean) = prefs.edit().putBoolean(KEY_VOICE_FRICTION, enabled).apply()

    fun setFrictionLanguage(language: FrictionLanguage) =
        prefs.edit().putString(KEY_FRICTION_LANGUAGE, language.code).apply()

    fun setHindiForm(form: HindiForm) = prefs.edit().putString(KEY_HINDI_FORM, form.code).apply()

    /** Adds or replaces a schedule (matched by id). */
    fun saveSchedule(schedule: FocusSchedule) {
        val updated = current.schedules.filterNot { it.id == schedule.id } + schedule
        prefs.edit().putString(KEY_SCHEDULES, FocusSchedules.encode(updated.sortedBy { it.startMinute })).apply()
    }

    fun deleteSchedule(id: Long) {
        prefs.edit().putString(KEY_SCHEDULES, FocusSchedules.encode(current.schedules.filterNot { it.id == id })).apply()
    }

    fun setDisclosureAccepted(accepted: Boolean) =
        prefs.edit().putBoolean(KEY_DISCLOSURE_ACCEPTED, accepted).apply()

    fun recordSwipe(today: LocalDate = LocalDate.now()) {
        val editor = prefs.edit()
        val (swipes, shields) = rollDay(editor, today)
        editor.putInt(KEY_SWIPES_TODAY, swipes + 1)
        writeHistory(editor, today, swipes + 1, shields)
        editor.apply()
    }

    /** Counts a raised shield; [blockedUntil] is set only for cooldown shields. */
    fun recordShield(blockedUntil: Long? = null, today: LocalDate = LocalDate.now()) {
        val editor = prefs.edit()
        val (swipes, shields) = rollDay(editor, today)
        editor.putInt(KEY_SHIELDS_TODAY, shields + 1)
        if (blockedUntil != null) editor.putLong(KEY_BLOCKED_UNTIL, blockedUntil)
        writeHistory(editor, today, swipes, shields + 1)
        editor.apply()
    }

    fun clearBlock() = prefs.edit().putLong(KEY_BLOCKED_UNTIL, 0L).apply()

    private fun writeHistory(editor: SharedPreferences.Editor, today: LocalDate, swipes: Int, shields: Int) {
        val updated = History.upsert(History.decode(prefs.getString(KEY_HISTORY, null)),
            DayStat(today.toEpochDay(), swipes, shields))
        editor.putString(KEY_HISTORY, History.encode(updated))
    }

    /** Returns today's (swipes, shields), resetting the counters in [editor] on a new day. */
    private fun rollDay(editor: SharedPreferences.Editor, today: LocalDate): Pair<Int, Int> {
        val day = today.toEpochDay()
        if (prefs.getLong(KEY_STATS_DAY, -1L) != day) {
            editor.putLong(KEY_STATS_DAY, day).putInt(KEY_SWIPES_TODAY, 0).putInt(KEY_SHIELDS_TODAY, 0)
            return 0 to 0
        }
        return prefs.getInt(KEY_SWIPES_TODAY, 0) to prefs.getInt(KEY_SHIELDS_TODAY, 0)
    }

    private fun read(): F11Settings {
        val today = LocalDate.now().toEpochDay()
        val isToday = prefs.getLong(KEY_STATS_DAY, -1L) == today
        val swipesToday = if (isToday) prefs.getInt(KEY_SWIPES_TODAY, 0) else 0
        val shieldsToday = if (isToday) prefs.getInt(KEY_SHIELDS_TODAY, 0) else 0
        // Today's live counters are the source of truth for today's history entry.
        val history = History.decode(prefs.getString(KEY_HISTORY, null)).let {
            if (swipesToday > 0 || shieldsToday > 0) History.upsert(it, DayStat(today, swipesToday, shieldsToday)) else it
        }
        return F11Settings(
            guardEnabled = prefs.getBoolean(KEY_GUARD_ENABLED, true),
            swipeLimit = prefs.getInt(KEY_SWIPE_LIMIT, DEFAULT_SWIPE_LIMIT).coerceIn(SWIPE_LIMIT_RANGE),
            cooldownMinutes = prefs.getInt(KEY_COOLDOWN_MINUTES, DEFAULT_COOLDOWN_MINUTES).coerceIn(COOLDOWN_RANGE),
            disabledPackages = prefs.getStringSet(KEY_DISABLED_PACKAGES, emptySet())?.toSet().orEmpty(),
            disclosureAccepted = prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false),
            blockShortFormCompletely = prefs.getBoolean(KEY_BLOCK_SHORT_FORM, false),
            maxFocus = prefs.getBoolean(KEY_MAX_FOCUS, false),
            showCounter = prefs.getBoolean(KEY_SHOW_COUNTER, true),
            voiceFriction = prefs.getBoolean(KEY_VOICE_FRICTION, false),
            frictionLanguage = FrictionLanguage.fromCode(prefs.getString(KEY_FRICTION_LANGUAGE, null)),
            hindiForm = HindiForm.fromCode(prefs.getString(KEY_HINDI_FORM, null)),
            swipesToday = swipesToday,
            shieldsToday = shieldsToday,
            blockedUntil = prefs.getLong(KEY_BLOCKED_UNTIL, 0L),
            history = history,
            today = today,
            firstDay = prefs.getLong(KEY_FIRST_DAY, today),
            schedules = FocusSchedules.decode(prefs.getString(KEY_SCHEDULES, null)),
        )
    }

    companion object {
        const val DEFAULT_SWIPE_LIMIT = 20
        const val DEFAULT_COOLDOWN_MINUTES = 15
        val SWIPE_LIMIT_RANGE = 5..100
        val COOLDOWN_RANGE = 5..120

        private const val FILE_NAME = "f11_prefs"
        private const val KEY_GUARD_ENABLED = "guard_enabled"
        private const val KEY_SWIPE_LIMIT = "swipe_limit"
        private const val KEY_COOLDOWN_MINUTES = "cooldown_minutes"
        private const val KEY_DISABLED_PACKAGES = "disabled_packages"
        private const val KEY_DISCLOSURE_ACCEPTED = "disclosure_accepted"
        private const val KEY_BLOCK_SHORT_FORM = "block_short_form_completely"
        private const val KEY_MAX_FOCUS = "max_focus"
        private const val KEY_SHOW_COUNTER = "show_counter"
        private const val KEY_VOICE_FRICTION = "voice_friction"
        private const val KEY_FRICTION_LANGUAGE = "friction_language"
        private const val KEY_HINDI_FORM = "hindi_form"
        private const val KEY_STATS_DAY = "stats_day"
        private const val KEY_SWIPES_TODAY = "swipes_today"
        private const val KEY_SHIELDS_TODAY = "shields_today"
        private const val KEY_BLOCKED_UNTIL = "blocked_until"
        private const val KEY_HISTORY = "history"
        private const val KEY_FIRST_DAY = "first_day"
        private const val KEY_SCHEDULES = "focus_schedules"

        @Volatile
        private var instance: PreferencesManager? = null

        fun get(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
    }
}
