package com.f11.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.f11.app.data.F11Settings
import com.f11.app.data.FocusSchedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import com.f11.app.data.PreferencesManager
import com.f11.app.data.TargetAppsConfig
import kotlin.math.roundToInt

/** Callbacks for every setting. Loosening ones are routed through friction by the caller. */
class SettingsActions(
    val onBlockShortForm: (Boolean) -> Unit,
    val onSwipeLimit: (Int) -> Unit,
    val onCooldown: (Int) -> Unit,
    val onAppGuarded: (String, String, Boolean) -> Unit,
    val onShowCounter: (Boolean) -> Unit,
    val onAddSchedule: () -> Unit,
    val onEditSchedule: (FocusSchedule) -> Unit,
    val onScheduleEnabled: (FocusSchedule, Boolean) -> Unit,
)

@Composable
fun SettingsScreen(
    settings: F11Settings,
    setup: SetupState,
    setupActions: SetupActions,
    actions: SettingsActions,
    onBack: () -> Unit,
) {
    // Swipe limits and the counter work only through the F11 Shield plugin.
    val swipes = setup.shieldOn
    val budgetActive = swipes && settings.guardEnabled && !settings.blockShortFormCompletely

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TopBar("Settings", left = { CircleIconButton("Back", onBack) { BackGlyph() } })

        SectionHeader("Reels & Shorts")
        ShieldPluginCard(setup, setupActions)
        Tile(Modifier.fillMaxWidth()) {
            if (!swipes) {
                Text("These need the F11 Shield plugin.", style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
                Divider()
            }
            ToggleRow("Block Reels & Shorts completely", "No budget. DMs, stories and regular videos stay open.",
                settings.blockShortFormCompletely, actions.onBlockShortForm, enabled = swipes && settings.guardEnabled)
            Divider()
            SettingSlider("Swipes per session", settings.swipeLimit, PreferencesManager.SWIPE_LIMIT_RANGE, 5,
                { "$it" }, actions.onSwipeLimit, enabled = budgetActive)
            SpacerH(10.dp)
            SettingSlider("Cooldown", settings.cooldownMinutes, PreferencesManager.COOLDOWN_RANGE, 5,
                { "$it min" }, actions.onCooldown, enabled = budgetActive)
        }

        SectionHeader("Focus schedule")
        FocusSetupCard(setup, setupActions)
        Tile(Modifier.fillMaxWidth()) {
            Text("Max Focus turns on by itself at these times and cannot be ended early.",
                style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
            val now = LocalDateTime.now()
            settings.schedules.forEach { schedule ->
                Divider()
                val running = schedule.isActive(now)
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = !running, role = Role.Button) { actions.onEditSchedule(schedule) }) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(schedule.name, style = MaterialTheme.typography.bodyLarge, color = F11Palette.Text)
                        Text(
                            if (running) "Running · locked until ${FocusSchedule.formatMinute(schedule.endMinute)}"
                            else "${FocusSchedule.formatMinute(schedule.startMinute)}–${FocusSchedule.formatMinute(schedule.endMinute)} · ${daysSummary(schedule.days)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (running) F11Palette.Ember else F11Palette.TextDim,
                        )
                    }
                    F11Switch(schedule.enabled, { actions.onScheduleEnabled(schedule, it) }, enabled = !running)
                }
            }
            Divider()
            Text("+  Add focus time", style = MaterialTheme.typography.bodyLarge, color = F11Palette.Violet,
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = actions.onAddSchedule).padding(vertical = 4.dp))
        }

        SectionHeader("Awareness")
        Tile(Modifier.fillMaxWidth()) {
            ToggleRow("Live swipe counter", "Shows today's count at the top of Reels and Shorts, even with the guard off.",
                settings.showCounter, actions.onShowCounter, enabled = swipes)
        }

        SectionHeader("Guarded feeds")
        Tile(Modifier.fillMaxWidth()) {
            TargetAppsConfig.apps.forEachIndexed { i, app ->
                if (i > 0) Divider()
                ToggleRow(app.displayName, app.surfaceName, settings.isAppGuarded(app.packageName),
                    { actions.onAppGuarded(app.packageName, app.displayName, it) })
            }
        }

        SectionHeader("Privacy")
        Tile(Modifier.fillMaxWidth()) {
            Text("F11 has no internet permission. It uses only app names (and, with the Shield plugin, view " +
                "ids), never text, and keeps only daily counts on this phone.",
                style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
        }
    }
}

private fun daysSummary(days: Set<DayOfWeek>): String {
    val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    return when (days) {
        DayOfWeek.entries.toSet() -> "Every day"
        weekdays -> "Mon–Fri"
        setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
        else -> days.sorted().joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = F11Palette.Hairline)
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) F11Palette.Text else F11Palette.TextFaint)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
        }
        F11Switch(checked, onChange, enabled)
    }
}

/** Slider that reports a value only when the drag ends; otherwise it mirrors the stored value. */
@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    range: IntRange,
    step: Int,
    format: (Int) -> String,
    onPicked: (Int) -> Unit,
    enabled: Boolean,
) {
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    val position = dragPosition ?: value.toFloat()

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) F11Palette.Text else F11Palette.TextFaint, modifier = Modifier.weight(1f))
        DotMatrixText(format(position.roundToInt()).filter { it.isDigit() }, dot = 2.5.dp,
            color = if (enabled) F11Palette.Text else F11Palette.TextFaint)
        if (format(0).contains("min")) {
            SpacerW(6.dp)
            Text("min", style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
        }
    }
    Slider(
        value = position,
        onValueChange = { dragPosition = it },
        onValueChangeFinished = {
            val picked = dragPosition?.roundToInt()
            dragPosition = null
            if (picked != null && picked != value) onPicked(picked)
        },
        enabled = enabled,
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (range.last - range.first) / step - 1,
        colors = SliderDefaults.colors(
            thumbColor = F11Palette.Text,
            activeTrackColor = F11Palette.Violet,
            inactiveTrackColor = F11Palette.TileHigh,
        ),
    )
}
