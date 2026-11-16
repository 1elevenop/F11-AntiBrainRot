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
 * Google Play "prominent disclosure" for a sensitive permission. Shown in-app, before the user
 * is sent to system settings, and requires an explicit affirmative tap.
 */
@Composable
fun ProminentDisclosureDialog(
    title: String,
    sections: List<Pair<String, String>>,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                sections.forEach { (heading, body) -> DisclosureSection(heading, body) }
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
