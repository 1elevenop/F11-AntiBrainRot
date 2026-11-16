package com.f11.app.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/** Where the optional F11 Shield plugin stands, as seen from either app. */
enum class ShieldState { NOT_INSTALLED, OFF, STALLED, ON }

object ShieldStatus {

    private val component = ComponentName(ShieldContract.SHIELD_PACKAGE, ShieldContract.SHIELD_SERVICE)

    fun state(context: Context): ShieldState = when {
        !isInstalled(context) -> ShieldState.NOT_INSTALLED
        isRunning(context) -> ShieldState.ON
        // Switched on in settings but not running: Android can drop it after an update.
        isSwitchedOn(context) -> ShieldState.STALLED
        else -> ShieldState.OFF
    }

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(ShieldContract.SHIELD_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun isRunning(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            val info = it.resolveInfo.serviceInfo
            info.packageName == component.packageName && info.name == component.className
        }
    }

    private fun isSwitchedOn(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
    }

    /**
     * Android 13+ locks the Accessibility switch ("Restricted setting") for apps that weren't
     * installed by an app store such as Google Play.
     */
    fun mayHaveRestrictedSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val installer = try {
            context.packageManager.getInstallSourceInfo(ShieldContract.SHIELD_PACKAGE).installingPackageName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return installer != PLAY_STORE
    }

    fun openAccessibilitySettings(context: Context) =
        open(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /** App info for the plugin, where "Allow restricted settings" lives. */
    fun openShieldAppInfo(context: Context) = open(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ShieldContract.SHIELD_PACKAGE, null)),
    )

    fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private const val PLAY_STORE = "com.android.vending"
}
