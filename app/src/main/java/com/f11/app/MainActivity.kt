package com.f11.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f11.app.data.PreferencesManager
import com.f11.app.data.ShieldState
import com.f11.app.data.ShieldStatus
import com.f11.app.focus.FocusWatchService
import com.f11.app.ui.F11App
import com.f11.app.ui.F11Theme
import com.f11.app.ui.ProminentDisclosureDialog
import com.f11.app.ui.SetupActions
import com.f11.app.ui.SetupState

class MainActivity : ComponentActivity() {

    private val prefs by lazy { PreferencesManager.get(this) }
    private var setup by mutableStateOf(SetupState())
    /** Set once the user was sent to Accessibility settings for the plugin. */
    private var askedForShieldSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askedForShieldSettings = savedInstanceState?.getBoolean(KEY_ASKED_FOR_SETTINGS) ?: false
        enableEdgeToEdge()
        setContent {
            F11Theme {
                val settings by prefs.settings.collectAsStateWithLifecycle()
                var showDisclosure by rememberSaveable { mutableStateOf(false) }

                // Keep the focus watcher running exactly while Max Focus or a schedule needs it.
                LaunchedEffect(settings, setup) { FocusWatchService.sync(this@MainActivity, settings) }

                F11App(
                    settings = settings,
                    prefs = prefs,
                    setup = setup,
                    setupActions = SetupActions(
                        onGrantUsageAccess = { showDisclosure = true },
                        onGrantOverlay = ::openOverlaySettings,
                        onShield = ::onShield,
                        onShieldAppInfo = { ShieldStatus.openShieldAppInfo(this) },
                    ),
                )

                if (showDisclosure) {
                    ProminentDisclosureDialog(
                        title = "F11 uses Usage access",
                        sections = USAGE_DISCLOSURE,
                        onAccept = {
                            showDisclosure = false
                            prefs.setDisclosureAccepted(true)
                            ShieldStatus.open(this, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        onDecline = { showDisclosure = false },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_ASKED_FOR_SETTINGS, askedForShieldSettings)
    }

    override fun onResume() {
        super.onResume()
        prefs.refresh() // e.g. a new day began while F11 was in the background
        updateSetup()
    }

    // The plugin's service can be re-bound a moment after the screen appears, so keep listening.
    private val stateListener = AccessibilityManager.AccessibilityStateChangeListener { updateSetup() }

    override fun onStart() {
        super.onStart()
        getSystemService(AccessibilityManager::class.java)?.addAccessibilityStateChangeListener(stateListener)
    }

    override fun onStop() {
        getSystemService(AccessibilityManager::class.java)?.removeAccessibilityStateChangeListener(stateListener)
        super.onStop()
    }

    private fun updateSetup() {
        val shield = ShieldStatus.state(this)
        setup = SetupState(
            usageAccess = FocusWatchService.hasUsageAccess(this),
            overlay = FocusWatchService.canDrawOverlay(this),
            shield = shield,
            // Only after a trip to Accessibility settings: Android offers "Allow restricted
            // settings" in App info once the user has tried the locked switch.
            showRestrictedHelp = askedForShieldSettings && shield == ShieldState.OFF &&
                ShieldStatus.mayHaveRestrictedSettings(this),
        )
    }

    private fun onShield() {
        when (setup.shield) {
            // F11 has no internet permission; the browser opens the download page.
            ShieldState.NOT_INSTALLED -> ShieldStatus.open(this, Intent(Intent.ACTION_VIEW, Uri.parse(SHIELD_DOWNLOAD_URL)))
            ShieldState.ON -> Unit
            else -> {
                askedForShieldSettings = true
                ShieldStatus.openAccessibilitySettings(this)
            }
        }
    }

    private fun openOverlaySettings() {
        requestNotificationsOnce()
        ShieldStatus.open(this, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", packageName, null)))
    }

    /** The focus watcher's notification is how Android shows it's running. */
    private fun requestNotificationsOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    private companion object {
        const val KEY_ASKED_FOR_SETTINGS = "asked_for_settings"
        const val SHIELD_DOWNLOAD_URL = "https://github.com/1elevenop/F11-AntiBrainRot/releases/latest"

        val USAGE_DISCLOSURE = listOf(
            "What F11 does" to
                "During Max Focus or a focus schedule, F11 covers the apps you chose to guard (Instagram, " +
                "YouTube, TikTok, Facebook, Snapchat) with a full-screen shield.",
            "What F11 accesses" to
                "Usage access tells F11 which app is in front, by app name only. F11 does not read what's " +
                "on screen, your messages or your browsing.",
            "Where the data goes" to
                "Nowhere. F11 has no internet permission, so it cannot send anything off your device. " +
                "Only daily shield counts are kept, in private app storage.",
            "Your control" to
                "You can remove Usage access at any time in Settings > Apps > Special app access.",
        )
    }
}
