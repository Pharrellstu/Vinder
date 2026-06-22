package com.example.vinted.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vinted.data.AccountPreferences

/**
 * Restarts the message listener after the device reboots so notifications resume without the user
 * having to open the app first. Only starts the service if a signed-in account is persisted;
 * [MessageNotificationController.ensureSession] still re-validates the auth session afterwards.
 *
 * `BOOT_COMPLETED` is one of the exemptions that permits starting a `dataSync` foreground service
 * from the background.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // AccountPreferences is initialised in VinderApplication.onCreate, which runs before this.
        if (AccountPreferences.load() == null) return
        MessageListenerService.start(context)
    }
}
