package com.f11.shield

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.f11.app.data.ShieldContract
import com.f11.app.data.ShieldState
import com.f11.app.data.ShieldStatus
import com.f11.app.ui.F11Palette
import com.f11.app.ui.F11Theme
import com.f11.app.ui.ProminentDisclosureDialog
import com.f11.app.ui.SpacerH
import com.f11.app.ui.Tile
import com.f11.app.ui.TileLabel

/** The plugin's only screen: is F11 installed, is the Shield on, and a way to turn it on. */
class StatusActivity : ComponentActivity() {

    private var state by mutableStateOf(ShieldState.OFF)
    private var f11Installed by mutableStateOf(false)
    private var askedForSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askedForSettings = savedInstanceState?.getBoolean(KEY_ASKED) ?: false
        enableEdgeToEdge()
        setContent {
            F11Theme {
                var showDisclosure by rememberSaveable { mutableStateOf(false) }
                StatusScreen(
                    state = state,
                    f11Installed = f11Installed,
                    showRestrictedHelp = askedForSettings && state == ShieldState.OFF &&
                        ShieldStatus.mayHaveRestrictedSettings(this),
                    onEnable = {
                        if (state == ShieldState.STALLED) openSettings() else showDisclosure = true
                    },
                    onOpenAppInfo = { ShieldStatus.openShieldAppInfo(this) },
                    onOpenF11 = {
                        packageManager.getLaunchIntentForPackage(ShieldContract.APP_PACKAGE)?.let(::startActivity)
                    },
                )
                if (showDisclosure) {
                    ProminentDisclosureDialog(
                        title = "F11 Shield uses the Accessibility API",
                        sections = ACCESSIBILITY_DISCLOSURE,
                        onAccept = {
                            showDisclosure = false
                            openSettings()
                        },
                        onDecline = { showDisclosure = false },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_ASKED, askedForSettings)
    }

    override fun onResume() {
        super.onResume()
        state = ShieldStatus.state(this)
        f11Installed = packageManager.getLaunchIntentForPackage(ShieldContract.APP_PACKAGE) != null
    }

    private fun openSettings() {
        askedForSettings = true
        ShieldStatus.openAccessibilitySettings(this)
    }

    private companion object {
        const val KEY_ASKED = "asked_for_settings"
    }
}

@androidx.compose.runtime.Composable
private fun StatusScreen(
    state: ShieldState,
    f11Installed: Boolean,
    showRestrictedHelp: Boolean,
    onEnable: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenF11: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(F11Palette.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("F11 Shield", style = MaterialTheme.typography.headlineMedium, color = F11Palette.Text)
        SpacerH(6.dp)
        Text("Adds Reels and Shorts swipe counting, swipe budgets and the live counter to F11.",
            style = MaterialTheme.typography.bodyMedium, color = F11Palette.TextDim)
        SpacerH(20.dp)

        if (!f11Installed) {
            Tile(Modifier.fillMaxWidth(), brush = F11Palette.Sunset) {
                TileLabel("Install F11 first")
                SpacerH(4.dp)
                Text("The Shield is a plugin. Your limits and counts live in the F11 app, so the Shield does nothing until F11 is installed.",
                    style = MaterialTheme.typography.bodySmall, color = F11Palette.Text)
            }
            SpacerH(16.dp)
        }

        Tile(Modifier.fillMaxWidth()) {
            TileLabel(when (state) {
                ShieldState.ON -> "Shield is on"
                ShieldState.STALLED -> "Shield stopped"
                else -> "Shield is off"
            }, color = if (state == ShieldState.ON) F11Palette.Mint else F11Palette.Text)
            SpacerH(4.dp)
            Text(when (state) {
                ShieldState.ON -> "Set your swipe budget and cooldown in F11."
                ShieldState.STALLED -> "Android stopped F11 Shield, often after an update. Turn it off and on again in Accessibility settings."
                else -> "Turn on F11 Shield in Accessibility settings."
            }, style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)
            if (showRestrictedHelp) {
                SpacerH(10.dp)
                Text("Switch greyed out or a “Restricted setting” message? Android locks it for apps " +
                    "installed outside the Play Store. Open App info, tap ⋮ at the top right, choose " +
                    "Allow restricted settings, then tap Turn on again.",
                    style = MaterialTheme.typography.bodySmall, color = F11Palette.Text)
            }
            if (state != ShieldState.ON) {
                SpacerH(12.dp)
                Button(onClick = onEnable, colors = ButtonDefaults.buttonColors(
                    containerColor = F11Palette.Text, contentColor = F11Palette.Black)) {
                    Text(if (state == ShieldState.STALLED) "Open settings" else "Turn on")
                }
                if (showRestrictedHelp) {
                    TextButton(onClick = onOpenAppInfo) { Text("Open App info", color = F11Palette.Text) }
                }
            }
        }

        SpacerH(16.dp)
        Text("Paying with a UPI app? Payment apps may refuse to work while any downloaded app has " +
            "Accessibility on. Turn F11 Shield off, restart, pay, then turn it back on.",
            style = MaterialTheme.typography.bodySmall, color = F11Palette.TextDim)

        if (f11Installed) {
            SpacerH(16.dp)
            TextButton(onClick = onOpenF11) { Text("Open F11", color = F11Palette.Violet) }
        }
    }
}

private val ACCESSIBILITY_DISCLOSURE = listOf(
    "What F11 Shield does" to
        "It counts your swipes in short-form video feeds (Instagram Reels, YouTube Shorts, TikTok, " +
        "Facebook Reels, Snapchat Spotlight) and covers the screen with a shield when you reach the " +
        "limit you set in F11. With Max Focus or a focus schedule on, it blocks those apps entirely.",
    "What it accesses" to
        "Which app is in the foreground, and the technical id of the list you scroll in those apps. " +
        "It does not read text, messages, passwords or videos, and does not record your screen.",
    "Where the data goes" to
        "Only to the F11 app on this phone. Neither app has internet permission, so nothing can be " +
        "sent off your device. Only daily swipe and shield counts are kept.",
    "Your control" to
        "You can turn F11 Shield off at any time in Settings > Accessibility, or uninstall it; F11 keeps working without it.",
)
