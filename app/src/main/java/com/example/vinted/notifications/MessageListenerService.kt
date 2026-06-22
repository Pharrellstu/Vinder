package com.example.vinted.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service that hosts the Supabase Realtime message listener so notifications keep
 * arriving while the app is backgrounded or the screen is locked. Without a foreground service
 * Android suspends the process, the Realtime socket dies, and only foreground delivery works.
 *
 * The actual listening logic lives in [MessageNotificationController]; this service only owns its
 * lifecycle and the persistent notification that keeps the process alive.
 *
 * Note: this does not survive the app being swiped away from recents, nor long Doze periods.
 */
class MessageListenerService : Service() {

    override fun onCreate() {
        super.onCreate()
        VinderNotifications.createChannels(this)
        ServiceCompat.startForeground(
            this,
            ONGOING_NOTIFICATION_ID,
            VinderNotifications.buildOngoingNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MessageNotificationController.start(applicationContext)
        // Best-effort restart by the OS if the process is reclaimed.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        MessageNotificationController.stop()
        super.onDestroy()
    }

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MessageListenerService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MessageListenerService::class.java))
        }
    }
}
