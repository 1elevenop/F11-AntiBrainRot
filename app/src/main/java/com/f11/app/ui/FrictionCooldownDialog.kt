package com.f11.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.f11.app.data.F11Settings
import com.f11.app.data.FrictionLines
import kotlinx.coroutines.delay

const val FRICTION_WAIT_SECONDS = 30

/**
 * Intentional friction for loosening any F11 rule:
 *  1. a timed pause,
 *  2. typing a hard-hitting line exactly.
 * Tightening rules never goes through this dialog.
 */
@Composable
fun FrictionCooldownDialog(
    changeDescription: String,
    settings: F11Settings,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    waitSeconds: Int = FRICTION_WAIT_SECONDS,
) {
    val typeLine = remember { FrictionLines.pick(1, settings.frictionLanguage, settings.hindiForm).first() }

    var secondsLeft by rememberSaveable { mutableIntStateOf(waitSeconds) }
    var typed by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
    }

    val typedOk = FrictionLines.typedMatches(typeLine, typed)
    val canConfirm = secondsLeft == 0 && typedOk

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(F11Palette.Surface)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text("PAUSE", style = MaterialTheme.typography.labelSmall, color = F11Palette.Violet)
            SpacerH(10.dp)
            Text("You're about to $changeDescription.", style = MaterialTheme.typography.bodyLarge, color = F11Palette.TextDim)
            SpacerH(20.dp)
            Text("“$typeLine”", style = MaterialTheme.typography.headlineSmall,
                color = F11Palette.Text, fontWeight = FontWeight.SemiBold)
            SpacerH(24.dp)

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                DotMatrixText(String.format("00:%02d", secondsLeft), dot = 3.dp,
                    color = if (secondsLeft == 0) F11Palette.Mint else F11Palette.Text)
                SpacerW(14.dp)
                Text(if (secondsLeft > 0) "sit with it" else "wait complete",
                    style = MaterialTheme.typography.labelLarge, color = F11Palette.TextDim)
            }
            SpacerH(8.dp)
            DotTrack(total = waitSeconds, lit = waitSeconds - secondsLeft, modifier = Modifier.fillMaxWidth())
            SpacerH(20.dp)

            Text("Type the line above", style = MaterialTheme.typography.labelLarge, color = F11Palette.TextDim)
            SpacerH(6.dp)
            FrictionField(typed, { typed = it }, done = typedOk)

            SpacerH(28.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = F11Palette.Violet, contentColor = F11Palette.Black),
                ) { Text("Keep my limits", fontWeight = FontWeight.SemiBold) }
                TextButton(
                    onClick = onConfirm,
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("Confirm change") }
            }
        }
    }
}

@Composable
private fun FrictionField(value: String, onChange: (String) -> Unit, done: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (done) F11Palette.Mint else F11Palette.Violet,
            unfocusedBorderColor = if (done) F11Palette.Mint else F11Palette.Hairline,
            focusedContainerColor = F11Palette.Tile,
            unfocusedContainerColor = F11Palette.Tile,
        ),
    )
}
