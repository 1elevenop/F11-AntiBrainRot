package com.f11.app.shield

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.f11.app.data.PreferencesManager
import com.f11.app.data.ShieldContract

/**
 * Lets the optional F11 Shield plugin read settings and report swipes. Protected by the
 * signature permission com.f11.permission.SHIELD (see the manifest), so only apps signed with
 * F11's own key can call it. Every call returns the fresh settings.
 */
class ShieldProvider : ContentProvider() {

    private val prefs by lazy { PreferencesManager.get(requireNotNull(context)) }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        // call() is not covered by the manifest's provider permission, so check it here.
        requireNotNull(context).enforceCallingOrSelfPermission(ShieldContract.PERMISSION, "F11 Shield only")
        when (method) {
            ShieldContract.METHOD_GET_SETTINGS -> prefs.refresh()
            ShieldContract.METHOD_RECORD_SWIPE -> prefs.recordSwipe()
            ShieldContract.METHOD_RECORD_SHIELD -> prefs.recordShield(
                extras?.takeIf { it.containsKey(ShieldContract.EXTRA_BLOCKED_UNTIL) }
                    ?.getLong(ShieldContract.EXTRA_BLOCKED_UNTIL),
            )
            ShieldContract.METHOD_CLEAR_BLOCK -> prefs.clearBlock()
            else -> throw IllegalArgumentException("Unknown method $method")
        }
        return prefs.rawBundle()
    }

    // Only call() is used.
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
