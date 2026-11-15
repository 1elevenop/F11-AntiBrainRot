package com.f11.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.f11.app.data.FocusSchedule
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private data class Preset(val name: String, val start: Int, val end: Int, val days: Set<DayOfWeek>)

private val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val PRESETS = listOf(
    Preset("Work", 9 * 60, 18 * 60, WEEKDAYS),
    Preset("Study", 18 * 60, 21 * 60, DayOfWeek.entries.toSet()),
    Preset("Sleep", 23 * 60, 7 * 60, DayOfWeek.entries.toSet()),
    Preset("Morning", 6 * 60, 9 * 60, DayOfWeek.entries.toSet()),
)

/** Create or edit a focus schedule. [onDelete] is null for a new schedule. */
@Composable
fun ScheduleEditorDialog(
    initial: FocusSchedule?,
    onSave: (FocusSchedule) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "Work") }
    var start by remember { mutableIntStateOf(initial?.startMinute ?: 9 * 60) }
    var end by remember { mutableIntStateOf(initial?.endMinute ?: 18 * 60) }
    var days by remember { mutableStateOf(initial?.days ?: WEEKDAYS) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(F11Palette.Surface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(if (initial == null) "NEW FOCUS TIME" else "EDIT FOCUS TIME",
                style = MaterialTheme.typography.labelSmall, color = F11Palette.Violet)
            SpacerH(16.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { p ->
                    Pill(p.name, selected = name == p.name) {
                        name = p.name; start = p.start; end = p.end; days = p.days
                    }
                }
            }
            SpacerH(14.dp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = F11Palette.Violet, unfocusedBorderColor = F11Palette.Hairline,
                    focusedContainerColor = F11Palette.Tile, unfocusedContainerColor = F11Palette.Tile,
                ),
            )
            SpacerH(18.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeBox("Starts", start, Modifier.weight(1f)) { picking = true }
                TimeBox("Ends", end, Modifier.weight(1f)) { picking = false }
            }
            if (end <= start) {
                SpacerH(6.dp)
                Text("Runs past midnight, into the next day.", style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
            }
            SpacerH(18.dp)
            Text("Days", style = MaterialTheme.typography.labelLarge, color = F11Palette.TextDim)
            SpacerH(8.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayOfWeek.entries.forEach { d ->
                    val on = d in days
                    Text(
                        d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) F11Palette.Black else F11Palette.Text,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (on) F11Palette.Text else F11Palette.TileHigh)
                            .clickable(role = Role.Checkbox) { days = if (on) days - d else days + d }
                            .padding(top = 9.dp),
                    )
                }
            }
            SpacerH(24.dp)
            val valid = name.isNotBlank() && days.isNotEmpty() && start != end
            Button(
                onClick = {
                    onSave(FocusSchedule(initial?.id ?: System.currentTimeMillis(), name.trim(), start, end, days, initial?.enabled ?: true))
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = F11Palette.Violet, contentColor = F11Palette.Black),
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = F11Palette.Ember) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = F11Palette.TextDim) }
            }
        }
    }

    picking?.let { isStart ->
        TimePickDialog(
            initialMinute = if (isStart) start else end,
            onPicked = { if (isStart) start = it else end = it; picking = null },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) F11Palette.Black else F11Palette.Text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) F11Palette.Text else F11Palette.TileHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun TimeBox(label: String, minute: Int, modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier, onClick = onClick) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
        SpacerH(10.dp)
        DotMatrixText(FocusSchedule.formatMinute(minute), dot = 3.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(initialMinute: Int, onPicked: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(F11Palette.Surface).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = F11Palette.Tile,
                    selectorColor = F11Palette.Violet,
                    timeSelectorSelectedContainerColor = F11Palette.Violet,
                    timeSelectorSelectedContentColor = F11Palette.Black,
                    timeSelectorUnselectedContainerColor = F11Palette.Tile,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = F11Palette.TextDim) }
                TextButton(onClick = { onPicked(state.hour * 60 + state.minute) }) { Text("OK", color = F11Palette.Violet) }
            }
        }
    }
}
