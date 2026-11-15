package com.f11.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.f11.app.data.F11Settings
import com.f11.app.data.FocusSchedule
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

private data class HeroPage(val title: String, val value: Int, val unit: String, val caption: String, val brush: Brush)

@Composable
fun HomeScreen(
    settings: F11Settings,
    serviceEnabled: Boolean,
    onEnableService: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onGuardToggle: (Boolean) -> Unit,
    onMaxFocusToggle: (Boolean) -> Unit,
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000L)
            value = System.currentTimeMillis()
        }
    }
    val pages = listOf(
        HeroPage("Today", settings.swipesToday, "swipes", "Scroll Awareness", F11Palette.Magenta),
        HeroPage("Today", settings.shieldsToday, "shields raised", "Shield Activity", F11Palette.Indigo),
        HeroPage("Streak", settings.focusStreak, if (settings.focusStreak == 1) "day" else "days", "Focus Streak", F11Palette.Sunset),
    )
    val pager = rememberPagerState { pages.size }
    val week = settings.lastDays(7)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        TopBar(
            title = "F11",
            left = { CircleIconButton("Report", onOpenHistory) { ChartGlyph() } },
            right = { CircleIconButton("Settings", onOpenSettings) { MenuGlyph() } },
        )

        if (!serviceEnabled) {
            Tile(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), brush = F11Palette.Sunset) {
                TileLabel("Shield is off")
                SpacerH(4.dp)
                Text("Turn on the F11 Shield so it can count and guard your feeds.",
                    style = MaterialTheme.typography.bodySmall, color = F11Palette.Text.copy(alpha = 0.8f))
                SpacerH(12.dp)
                Button(onClick = onEnableService, colors = ButtonDefaults.buttonColors(
                    containerColor = F11Palette.Text, contentColor = F11Palette.Black)) { Text("Enable") }
            }
            SpacerH(16.dp)
        }

        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 14.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            val page = pages[index]
            GradientCard(page.brush, Modifier.fillMaxWidth().height(340.dp)) {
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text(page.title, style = MaterialTheme.typography.titleMedium, color = F11Palette.Text.copy(alpha = 0.9f))
                    Spacer(Modifier.weight(0.8f))
                    DotMatrixText(page.value.toString(), dot = 7.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    SpacerH(10.dp)
                    Text(page.unit, style = MaterialTheme.typography.bodyMedium, color = F11Palette.Text.copy(alpha = 0.75f),
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.weight(1f))
                    ArcDots(week.takeLast(5).map { if (index == 1) it.shields else it.swipes },
                        Modifier.fillMaxWidth().height(70.dp))
                }
            }
        }
        SpacerH(18.dp)
        Text(pages[pager.currentPage].caption, style = MaterialTheme.typography.titleSmall,
            color = F11Palette.Text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        SpacerH(10.dp)
        PageDots(pages.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally))

        if (settings.guardEnabled && settings.blockedUntil > now) {
            SpacerH(12.dp)
            val until = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(settings.blockedUntil))
            Text("Reels & Shorts unlock at $until", style = MaterialTheme.typography.labelLarge,
                color = F11Palette.Violet, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        SpacerH(24.dp)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f).height(214.dp), onClick = onOpenHistory) {
                TileLabel("Last 7 Days")
                SpacerH(4.dp)
                Text("${week.sumOf { it.swipes }} swipes", style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
                Spacer(Modifier.weight(1f))
                DotColumnChart(week.map { it.swipes }, Modifier.fillMaxWidth().height(110.dp), rows = 9)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(Modifier.fillMaxWidth().height(102.dp), brush = F11Palette.Indigo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            TileLabel("Guard")
                            Text(if (settings.guardEnabled) "${settings.swipeLimit}/session" else "counting only",
                                style = MaterialTheme.typography.bodySmall, color = F11Palette.Text.copy(alpha = 0.7f))
                        }
                        F11Switch(settings.guardEnabled, onGuardToggle)
                    }
                }
                val scheduled = if (settings.maxFocus) null else settings.activeSchedule()
                val focusOn = settings.maxFocus || scheduled != null
                Tile(
                    Modifier.fillMaxWidth().height(102.dp),
                    brush = if (focusOn) F11Palette.Sunset else F11Palette.Graphite,
                    onClick = if (scheduled != null) null else ({ onMaxFocusToggle(!settings.maxFocus) }),
                ) {
                    TileLabel("Max Focus")
                    Spacer(Modifier.weight(1f))
                    Text(
                        when {
                            scheduled != null -> "${scheduled.name} · until ${FocusSchedule.formatMinute(scheduled.endMinute)}"
                            settings.maxFocus -> "On · tap to end"
                            else -> "Off · tap to start"
                        },
                        style = MaterialTheme.typography.bodySmall, color = F11Palette.Text.copy(alpha = 0.75f))
                }
            }
        }
        SpacerH(18.dp)
        Text("100% OFFLINE · NO INTERNET PERMISSION", style = MaterialTheme.typography.labelSmall,
            color = F11Palette.TextFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun TopBar(title: String, left: @Composable () -> Unit, right: @Composable () -> Unit = { Box(Modifier.size(48.dp)) }) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left()
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        right()
    }
}
