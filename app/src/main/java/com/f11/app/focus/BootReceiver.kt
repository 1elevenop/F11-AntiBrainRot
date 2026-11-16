package com.f11.app.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.f11.app.data.PreferencesManager

/** Re-arms Max Focus and focus schedules after a restart. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        FocusWatchService.sync(context, PreferencesManager.get(context).current)
    }
}
