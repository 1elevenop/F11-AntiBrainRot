package com.f11.app.data

import android.net.Uri

/**
 * How the optional F11 Shield plugin talks to F11. F11 owns all data; the plugin reads settings
 * and reports swipes through F11's ContentProvider. The provider is guarded by a signature
 * permission, so only apps signed with F11's key can use it. Nothing leaves the device.
 */
object ShieldContract {
    const val APP_PACKAGE = "com.f11.app"
    const val SHIELD_PACKAGE = "com.f11.shield"
    const val SHIELD_SERVICE = "com.f11.shield.F11AccessibilityService"

    /** Signature permission defined by F11; the plugin must hold it. */
    const val PERMISSION = "com.f11.permission.SHIELD"

    const val AUTHORITY = "com.f11.app.shield"
    val SETTINGS_URI: Uri = Uri.parse("content://$AUTHORITY/settings")

    /** Returns every stored value as a Bundle; read it with [BundleSettingsSource]. */
    const val METHOD_GET_SETTINGS = "getSettings"
    const val METHOD_RECORD_SWIPE = "recordSwipe"
    /** Optional extra [EXTRA_BLOCKED_UNTIL] for cooldown shields. */
    const val METHOD_RECORD_SHIELD = "recordShield"
    const val METHOD_CLEAR_BLOCK = "clearBlock"

    const val EXTRA_BLOCKED_UNTIL = "blocked_until"
}
