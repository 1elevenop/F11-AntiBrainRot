package com.f11.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed store: F11's single source of truth. The dashboard and the focus
 * watcher use it directly; the optional Shield plugin reaches it through [ShieldProvider].
 * Every change is pushed to [settings] and announced on [ShieldContract.SETTINGS_URI].
 */
class PreferencesManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val source = object : SettingsSource {
        override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
        override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
        override fun getLong(key: String, default: Long) = prefs.getLong(key, default)
        override fun getString(key: String): String? = prefs.getString(key, null)
        override fun getStringSet(key: String): Set<String>? = prefs.getStringSet(key, null)?.toSet()
    }

    init {
        if (!prefs.contains(SettingsKeys.FIRST_DAY)) {
            prefs.edit().putLong(SettingsKeys.FIRST_DAY, LocalDate.now().toEpochDay()).apply()
        }
    }

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<F11Settings> = _settings.asStateFlow()

    val current: F11Settings get() = _settings.value

    /**
     * Re-reads storage into [settings]. Every write goes through this class, so it refreshes
     * itself after writing instead of relying on an OnSharedPreferenceChangeListener: those are
     * held weakly, and R8 may drop the field that keeps one alive in release builds.
     */
    fun refresh() {
        _settings.value = read()
    }

    /** Re-reads storage and tells the Shield plugin (if any) that settings changed. */
    private fun changed() {
        refresh()
        appContext.contentResolver.notifyChange(ShieldContract.SETTINGS_URI, null)
    }

    /** Every stored value, for the Shield plugin. Read it back with [BundleSettingsSource]. */
    fun rawBundle(): Bundle = Bundle().apply {
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is String -> putString(key, value)
                is Set<*> -> putStringArrayList(key, ArrayList(value.filterIsInstance<String>()))
            }
        }
    }

    private inline fun write(block: (SharedPreferences.Editor) -> Unit) {
        val editor = prefs.edit()
        block(editor)
        editor.apply()
        changed()
    }

    fun setGuardEnabled(enabled: Boolean) = write { it.putBoolean(SettingsKeys.GUARD_ENABLED, enabled) }

    fun setSwipeLimit(limit: Int) =
        write { it.putInt(SettingsKeys.SWIPE_LIMIT, limit.coerceIn(SWIPE_LIMIT_RANGE)) }

    fun setCooldownMinutes(minutes: Int) =
        write { it.putInt(SettingsKeys.COOLDOWN_MINUTES, minutes.coerceIn(COOLDOWN_RANGE)) }

    fun setAppGuarded(packageName: String, guarded: Boolean) {
        val updated = current.disabledPackages.toMutableSet().apply {
            if (guarded) remove(packageName) else add(packageName)
        }
        write { it.putStringSet(SettingsKeys.DISABLED_PACKAGES, updated) }
    }

    fun setBlockShortFormCompletely(enabled: Boolean) =
        write { it.putBoolean(SettingsKeys.BLOCK_SHORT_FORM, enabled) }

    fun setMaxFocus(enabled: Boolean) = write { it.putBoolean(SettingsKeys.MAX_FOCUS, enabled) }

    fun setShowCounter(enabled: Boolean) = write { it.putBoolean(SettingsKeys.SHOW_COUNTER, enabled) }


    fun setFrictionLanguage(language: FrictionLanguage) =
        write { it.putString(SettingsKeys.FRICTION_LANGUAGE, language.code) }

    fun setHindiForm(form: HindiForm) = write { it.putString(SettingsKeys.HINDI_FORM, form.code) }

    /** Adds or replaces a schedule (matched by id). */
    fun saveSchedule(schedule: FocusSchedule) {
        val updated = current.schedules.filterNot { it.id == schedule.id } + schedule
        write { it.putString(SettingsKeys.SCHEDULES, FocusSchedules.encode(updated.sortedBy { it.startMinute })) }
    }

    fun deleteSchedule(id: Long) {
        write { it.putString(SettingsKeys.SCHEDULES, FocusSchedules.encode(current.schedules.filterNot { it.id == id })) }
    }

    fun setDisclosureAccepted(accepted: Boolean) =
        write { it.putBoolean(SettingsKeys.DISCLOSURE_ACCEPTED, accepted) }

    @Synchronized
    fun recordSwipe(today: LocalDate = LocalDate.now()) {
        val editor = prefs.edit()
        val (swipes, shields) = rollDay(editor, today)
        editor.putInt(SettingsKeys.SWIPES_TODAY, swipes + 1)
        writeHistory(editor, today, swipes + 1, shields)
        editor.apply()
        changed()
    }

    /** Counts a raised shield; [blockedUntil] is set only for cooldown shields. */
    @Synchronized
    fun recordShield(blockedUntil: Long? = null, today: LocalDate = LocalDate.now()) {
        val editor = prefs.edit()
        val (swipes, shields) = rollDay(editor, today)
        editor.putInt(SettingsKeys.SHIELDS_TODAY, shields + 1)
        if (blockedUntil != null) editor.putLong(SettingsKeys.BLOCKED_UNTIL, blockedUntil)
        writeHistory(editor, today, swipes, shields + 1)
        editor.apply()
        changed()
    }

    fun clearBlock() = write { it.putLong(SettingsKeys.BLOCKED_UNTIL, 0L) }

    private fun writeHistory(editor: SharedPreferences.Editor, today: LocalDate, swipes: Int, shields: Int) {
        val updated = History.upsert(History.decode(prefs.getString(SettingsKeys.HISTORY, null)),
            DayStat(today.toEpochDay(), swipes, shields))
        editor.putString(SettingsKeys.HISTORY, History.encode(updated))
    }

    /** Returns today's (swipes, shields), resetting the counters in [editor] on a new day. */
    private fun rollDay(editor: SharedPreferences.Editor, today: LocalDate): Pair<Int, Int> {
        val day = today.toEpochDay()
        if (prefs.getLong(SettingsKeys.STATS_DAY, -1L) != day) {
            editor.putLong(SettingsKeys.STATS_DAY, day).putInt(SettingsKeys.SWIPES_TODAY, 0).putInt(SettingsKeys.SHIELDS_TODAY, 0)
            return 0 to 0
        }
        return prefs.getInt(SettingsKeys.SWIPES_TODAY, 0) to prefs.getInt(SettingsKeys.SHIELDS_TODAY, 0)
    }

    private fun read(): F11Settings = SettingsKeys.read(source)

    companion object {
        val SWIPE_LIMIT_RANGE = SettingsKeys.SWIPE_LIMIT_RANGE
        val COOLDOWN_RANGE = SettingsKeys.COOLDOWN_RANGE

        private const val FILE_NAME = "f11_prefs"

        @Volatile
        private var instance: PreferencesManager? = null

        fun get(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
    }
}
