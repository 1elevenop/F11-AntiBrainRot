package com.f11.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.f11.app.data.PreferencesManager
import com.f11.app.service.F11AccessibilityService
import com.f11.app.ui.F11App
import com.f11.app.ui.F11Theme
import com.f11.app.ui.ProminentDisclosureDialog

class MainActivity : ComponentActivity() {

    private val prefs by lazy { PreferencesManager.get(this) }
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            F11Theme {
                val settings by prefs.settings.collectAsStateWithLifecycle()
                var showDisclosure by rememberSaveable { mutableStateOf(false) }

                F11App(
                    settings = settings,
                    prefs = prefs,
                    serviceEnabled = serviceEnabled,
                    onEnableService = { showDisclosure = true },
                )

                if (showDisclosure) {
                    ProminentDisclosureDialog(
                        onAccept = {
                            showDisclosure = false
                            prefs.setDisclosureAccepted(true)
                            openAccessibilitySettings()
                        },
                        onDecline = { showDisclosure = false },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = isF11ServiceEnabled()
    }

    // Android may re-bind the service a moment after the screen appears (e.g. after an update),
    // so keep listening instead of checking only once.
    private val stateListener = AccessibilityManager.AccessibilityStateChangeListener {
        serviceEnabled = isF11ServiceEnabled()
    }
    private val servicesListener: Any? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AccessibilityManager.AccessibilityServicesStateChangeListener { serviceEnabled = isF11ServiceEnabled() }
        } else {
            null
        }
    }

    override fun onStart() {
        super.onStart()
        val manager = getSystemService(AccessibilityManager::class.java) ?: return
        manager.addAccessibilityStateChangeListener(stateListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.addAccessibilityServicesStateChangeListener(
                mainExecutor, servicesListener as AccessibilityManager.AccessibilityServicesStateChangeListener,
            )
        }
    }

    override fun onStop() {
        val manager = getSystemService(AccessibilityManager::class.java)
        manager?.removeAccessibilityStateChangeListener(stateListener)
        if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.removeAccessibilityServicesStateChangeListener(
                servicesListener as AccessibilityManager.AccessibilityServicesStateChangeListener,
            )
        }
        super.onStop()
    }

    private fun isF11ServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val info = it.resolveInfo.serviceInfo
                info.packageName == packageName && info.name == F11AccessibilityService::class.java.name
            }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
