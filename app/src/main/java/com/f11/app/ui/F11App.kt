package com.f11.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.f11.app.data.F11Settings
import com.f11.app.data.FocusSchedule
import java.time.LocalDateTime
import com.f11.app.data.PreferencesManager

/** A loosening change waiting for the user to get through the friction dialog. */
private class PendingRelax(val description: String, val apply: () -> Unit)

private enum class Screen { HOME, HISTORY, SETTINGS }

/** The schedule editor is open; [schedule] is null when adding a new one. */
private class ScheduleEdit(val schedule: FocusSchedule?)

/** Root: simple three-screen navigation plus the shared friction gate. */
@Composable
fun F11App(
    settings: F11Settings,
    prefs: PreferencesManager,
    setup: SetupState,
    setupActions: SetupActions,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var pending by remember { mutableStateOf<PendingRelax?>(null) }
    var editor by remember { mutableStateOf<ScheduleEdit?>(null) }
    fun relax(description: String, apply: () -> Unit) {
        pending = PendingRelax(description, apply)
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    CompositionLocalProvider(LocalContentColor provides F11Palette.Text) {
    Box(Modifier.fillMaxSize().background(F11Palette.Black)) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                settings = settings,
                setup = setup,
                setupActions = setupActions,
                onOpenHistory = { screen = Screen.HISTORY },
                onOpenSettings = { screen = Screen.SETTINGS },
                onGuardToggle = { on ->
                    if (on) prefs.setGuardEnabled(true)
                    else relax("turn the guard off (swipes will still be counted)") { prefs.setGuardEnabled(false) }
                },
                onMaxFocusToggle = { on ->
                    // While a schedule runs, Max Focus cannot be ended from here.
                    if (!settings.maxFocus && settings.activeSchedule() != null) Unit
                    else if (on) prefs.setMaxFocus(true)
                    else relax("end Max Focus and unblock your apps") { prefs.setMaxFocus(false) }
                },
            )
            Screen.HISTORY -> HistoryScreen(settings, onBack = { screen = Screen.HOME })
            Screen.SETTINGS -> SettingsScreen(
                settings = settings,
                setup = setup,
                setupActions = setupActions,
                onBack = { screen = Screen.HOME },
                actions = SettingsActions(
                    onBlockShortForm = { on ->
                        if (on) prefs.setBlockShortFormCompletely(true)
                        else relax("allow Reels and Shorts again (with a swipe budget)") { prefs.setBlockShortFormCompletely(false) }
                    },
                    onSwipeLimit = { v ->
                        if (v <= settings.swipeLimit) prefs.setSwipeLimit(v)
                        else relax("raise your swipe limit from ${settings.swipeLimit} to $v") { prefs.setSwipeLimit(v) }
                    },
                    onCooldown = { v ->
                        if (v >= settings.cooldownMinutes) prefs.setCooldownMinutes(v)
                        else relax("shorten the cooldown from ${settings.cooldownMinutes} to $v minutes") { prefs.setCooldownMinutes(v) }
                    },
                    onAppGuarded = { pkg, name, on ->
                        if (on) prefs.setAppGuarded(pkg, true)
                        else relax("stop guarding $name") { prefs.setAppGuarded(pkg, false) }
                    },
                    onShowCounter = { on ->
                        if (on) prefs.setShowCounter(true)
                        else relax("hide the live swipe counter") { prefs.setShowCounter(false) }
                    },
                    onAddSchedule = { editor = ScheduleEdit(null) },
                    onEditSchedule = { editor = ScheduleEdit(it) },
                    onScheduleEnabled = { schedule, on ->
                        if (schedule.isActive(LocalDateTime.now())) Unit // locked while running
                        else if (on) prefs.saveSchedule(schedule.copy(enabled = true))
                        else relax("turn off your ${schedule.name} focus time") { prefs.saveSchedule(schedule.copy(enabled = false)) }
                    },
                ),
            )
        }

        editor?.let { edit ->
            val initial = edit.schedule
            ScheduleEditorDialog(
                initial = initial,
                onSave = { updated ->
                    editor = null
                    if (initial == null) prefs.saveSchedule(updated)
                    else relax("change your ${initial.name} focus time") { prefs.saveSchedule(updated) }
                },
                onDelete = initial?.let { s ->
                    {
                        editor = null
                        relax("delete your ${s.name} focus time") { prefs.deleteSchedule(s.id) }
                    }
                },
                onDismiss = { editor = null },
            )
        }

        pending?.let { change ->
            FrictionCooldownDialog(
                changeDescription = change.description,
                settings = settings,
                onConfirm = {
                    change.apply()
                    pending = null
                },
                onCancel = { pending = null },
            )
        }
    }
    }
}
