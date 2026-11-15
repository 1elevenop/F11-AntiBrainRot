package com.f11.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.f11.app.data.F11Settings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Report: totals, a dot chart and a per-day history for the last 7 or 30 days. */
@Composable
fun HistoryScreen(settings: F11Settings, onBack: () -> Unit) {
    var range by rememberSaveable { mutableIntStateOf(7) }
    val days = settings.lastDays(range)
    val total = days.sumOf { it.swipes }
    val shields = days.sumOf { it.shields }
    val average = total / range.coerceAtLeast(1)
    val busiest = days.maxByOrNull { it.swipes }?.takeIf { it.swipes > 0 }
    val dayName = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dayLabel = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        TopBar("Report", left = { CircleIconButton("Back", onBack) { BackGlyph() } })

        Row(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(50)).background(F11Palette.Tile)) {
            listOf(7, 30).forEach { r ->
                Text(
                    "${r}D",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (r == range) F11Palette.Black else F11Palette.TextDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (r == range) F11Palette.Text else F11Palette.Tile)
                        .clickable(role = Role.Tab) { range = r }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                )
            }
        }

        SpacerH(16.dp)
        GradientCard(F11Palette.Magenta, Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(300.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("Swipes · last $range days", style = MaterialTheme.typography.titleSmall, color = F11Palette.Text.copy(alpha = 0.85f))
                SpacerH(18.dp)
                DotMatrixText(total.toString(), dot = 6.dp)
                SpacerH(22.dp)
                DotColumnChart(days.map { it.swipes }, Modifier.fillMaxWidth().height(120.dp), rows = 10)
            }
        }

        SpacerH(12.dp)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Daily average", average.toString(), "swipes", Modifier.weight(1f))
            StatTile("Busiest day", busiest?.swipes?.toString() ?: "0",
                busiest?.let { LocalDate.ofEpochDay(it.epochDay).format(dayName) } ?: "none yet", Modifier.weight(1f))
        }
        SpacerH(10.dp)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Shields", shields.toString(), "raised", Modifier.weight(1f))
            StatTile("Focus streak", settings.focusStreak.toString(), "days", Modifier.weight(1f))
        }

        SpacerH(10.dp)
        SectionHeader("History", Modifier.padding(horizontal = 16.dp))
        val maxDay = days.maxOfOrNull { it.swipes }?.coerceAtLeast(1) ?: 1
        Tile(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            days.reversed().forEachIndexed { i, d ->
                if (i > 0) SpacerH(12.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (d.epochDay == settings.today) "Today" else LocalDate.ofEpochDay(d.epochDay).format(dayLabel),
                        style = MaterialTheme.typography.bodyMedium, color = F11Palette.Text, modifier = Modifier.width(96.dp),
                    )
                    DotTrack(total = 16, lit = (d.swipes * 16 + maxDay - 1) / maxDay, modifier = Modifier.weight(1f))
                    SpacerW(10.dp)
                    Text("${d.swipes}", style = MaterialTheme.typography.bodyMedium, color = F11Palette.Text,
                        textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                    Text(if (d.shields > 0) " · ${d.shields}⛨" else "", style = MaterialTheme.typography.bodySmall,
                        color = F11Palette.Violet, modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Tile(modifier.height(132.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        DotMatrixText(value, dot = 4.dp)
        SpacerH(6.dp)
        Text(unit, style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
    }
}
