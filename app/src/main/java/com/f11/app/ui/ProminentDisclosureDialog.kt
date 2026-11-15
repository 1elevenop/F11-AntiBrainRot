package com.f11.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Google Play "prominent disclosure" for the Accessibility API. Shown in-app, before the user
 * is sent to system settings, and requires an explicit affirmative tap.
 */
@Composable
fun ProminentDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("F11 uses the Accessibility API") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DisclosureSection(
                    "What F11 does",
                    "F11 counts your swipes in short-form video feeds (Instagram Reels, YouTube " +
                        "Shorts, TikTok, Facebook Reels, Snapchat Spotlight) and covers the screen " +
                        "with a shield when you reach the limit you set. With Max Focus or a focus " +
                        "schedule on, it blocks those apps entirely.",
                )
                DisclosureSection(
                    "What F11 accesses",
                    "Which app is in the foreground, and the technical id of the list you scroll " +
                        "in those apps. F11 does not read text, messages, passwords or videos, and " +
                        "does not record your screen.",
                )
                DisclosureSection(
                    "Where the data goes",
                    "Nowhere. F11 has no internet permission, so it physically cannot send " +
                        "anything off your device. Only daily swipe and shield counts are kept, in " +
                        "private app storage.",
                )
                DisclosureSection(
                    "Your control",
                    "You can turn the F11 Shield off at any time in Settings > Accessibility.",
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) { Text("Agree and continue") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("No thanks") }
        },
    )
}

@Composable
private fun DisclosureSection(heading: String, body: String) {
    Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
}
