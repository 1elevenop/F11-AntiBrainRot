package com.f11.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.f11.app.data.ShieldState

/** What F11 has been allowed to do, and where the optional Shield plugin stands. */
data class SetupState(
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val shield: ShieldState = ShieldState.NOT_INSTALLED,
    /** Android 13+ may have locked the plugin's Accessibility switch. */
    val showRestrictedHelp: Boolean = false,
) {
    val focusReady: Boolean get() = usageAccess && overlay
    val shieldOn: Boolean get() = shield == ShieldState.ON
}

class SetupActions(
    val onGrantUsageAccess: () -> Unit,
    val onGrantOverlay: () -> Unit,
    /** Get the plugin, or turn it on, depending on its state. */
    val onShield: () -> Unit,
    val onShieldAppInfo: () -> Unit,
)

/** Asks for the two permissions Max Focus and focus schedules need. */
@Composable
fun FocusSetupCard(setup: SetupState, actions: SetupActions, modifier: Modifier = Modifier) {
    if (setup.focusReady) return
    Tile(modifier.fillMaxWidth(), brush = F11Palette.Sunset) {
        TileLabel("Let F11 block apps")
        SpacerH(4.dp)
        Text("Max Focus and focus schedules need two permissions: Usage access, to see which app is " +
            "open, and Display over other apps, to cover a guarded app. Both stay on this phone.",
            style = MaterialTheme.typography.bodySmall, color = F11Palette.Text.copy(alpha = 0.8f))
        SpacerH(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!setup.usageAccess) {
                Button(onClick = actions.onGrantUsageAccess, colors = ButtonDefaults.buttonColors(
                    containerColor = F11Palette.Text, contentColor = F11Palette.Black)) { Text("Usage access") }
                SpacerW(8.dp)
            }
            if (!setup.overlay) {
                Button(onClick = actions.onGrantOverlay, colors = ButtonDefaults.buttonColors(
                    containerColor = F11Palette.Text, contentColor = F11Palette.Black)) { Text("Display over apps") }
            }
        }
    }
}

/** The optional F11 Shield plugin: what it adds, and how to get it running. */
@Composable
fun ShieldPluginCard(setup: SetupState, actions: SetupActions, modifier: Modifier = Modifier) {
    Tile(modifier.fillMaxWidth()) {
        TileLabel(
            when (setup.shield) {
                ShieldState.NOT_INSTALLED -> "F11 Shield plugin (optional)"
                ShieldState.OFF -> "F11 Shield is off"
                ShieldState.STALLED -> "F11 Shield stopped"
                ShieldState.ON -> "F11 Shield is on"
            },
            color = if (setup.shieldOn) F11Palette.Mint else F11Palette.Text,
        )
        SpacerH(4.dp)
        Text(
            when (setup.shield) {
                ShieldState.NOT_INSTALLED -> "Adds Reels and Shorts swipe counting, a swipe budget with cooldowns, and " +
                    "the live counter. It uses Android's Accessibility API, so Google Play Protect may block it when " +
                    "installed from a browser, and UPI apps may pause payments while it's on."
                ShieldState.OFF -> "Turn on F11 Shield in Accessibility settings to count swipes in Reels and Shorts."
                ShieldState.STALLED -> "Android stopped F11 Shield, often after an update. Turn it off and on again in Accessibility settings."
                ShieldState.ON -> "Counting swipes in Reels and Shorts. To pay with a UPI app, turn it off, restart, pay, then turn it back on."
            },
            style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim,
        )
        if (setup.showRestrictedHelp) {
            SpacerH(10.dp)
            Text("Switch greyed out or a “Restricted setting” message? Android locks it for apps installed " +
                "outside the Play Store. Open App info, tap ⋮ at the top right, choose Allow restricted " +
                "settings, then tap Turn on again.",
                style = MaterialTheme.typography.bodySmall, color = F11Palette.Text)
        }
        if (!setup.shieldOn) {
            SpacerH(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = actions.onShield, colors = ButtonDefaults.buttonColors(
                    containerColor = F11Palette.Text, contentColor = F11Palette.Black)) {
                    Text(when (setup.shield) {
                        ShieldState.NOT_INSTALLED -> "Get the plugin"
                        ShieldState.STALLED -> "Open settings"
                        else -> "Turn on"
                    })
                }
                if (setup.showRestrictedHelp) {
                    SpacerW(8.dp)
                    TextButton(onClick = actions.onShieldAppInfo) { Text("Open App info", color = F11Palette.Text) }
                }
            }
        }
    }
}
