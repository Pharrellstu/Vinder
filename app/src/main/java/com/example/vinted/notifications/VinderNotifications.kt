package com.example.vinted.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vinted.MainActivity
import com.example.vinted.R
import com.example.vinted.data.NotificationPreferences
import com.example.vinted.ui.models.NotificationType

/**
 * Posts system notifications, respecting both the OS-level permission and the user's
 * per-type preferences in [NotificationPreferences].
 *
 * One channel is created per notification group so the user can also tune them from the
 * Android system settings.
 */
object VinderNotifications {

    /** Group backing the persistent foreground-service notification. */
    const val BACKGROUND_GROUP = "Background"

    private val channelImportance = mapOf(
        "Messages" to NotificationManager.IMPORTANCE_HIGH,
        "Activity" to NotificationManager.IMPORTANCE_DEFAULT,
        "Marketing" to NotificationManager.IMPORTANCE_LOW,
        BACKGROUND_GROUP to NotificationManager.IMPORTANCE_MIN,
    )

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        channelImportance.forEach { (group, importance) ->
            val channel = NotificationChannel(channelId(group), group, importance).apply {
                description = "Vinder $group notifications"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** True when the app is allowed to post notifications at the OS level. */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Posts a notification for [type] if the user has it enabled and the OS permission is
     * granted. No-ops otherwise.
     */
    @SuppressLint("MissingPermission") // Guarded by hasPermission() above.
    fun notify(context: Context, type: NotificationType, title: String, body: String, id: Int) {
        if (!NotificationPreferences.isEnabled(type)) return
        if (!hasPermission(context)) return

        val notification = NotificationCompat.Builder(context, channelId(type.group))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /**
     * Builds the silent, ongoing notification shown while [MessageListenerService] keeps the
     * Realtime message listener alive in the background. Tapping it opens [MainActivity].
     */
    fun buildOngoingNotification(context: Context): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId(BACKGROUND_GROUP))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Listening for new messages")
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
    }

    private fun channelId(group: String) = "vinder_${group.lowercase()}"
}
