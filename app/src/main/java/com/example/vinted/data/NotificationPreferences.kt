package com.example.vinted.data

import android.content.Context
import android.content.SharedPreferences
import com.example.vinted.ui.models.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Persists the user's notification toggles in [SharedPreferences] and exposes them as a
 * reactive [StateFlow] for the UI. The notification delivery layer reads the same values
 * synchronously via [isEnabled].
 *
 * Initialise once from the application context before the Settings screen or the message
 * listener is used (see MainActivity).
 */
object NotificationPreferences {

    private const val PREFS_NAME = "vinder_notification_prefs"

    private var prefs: SharedPreferences? = null

    private val _enabled = MutableStateFlow(defaults())
    val enabled: StateFlow<Map<NotificationType, Boolean>> = _enabled.asStateFlow()

    private fun defaults(): Map<NotificationType, Boolean> =
        NotificationType.entries.associateWith { it.defaultEnabled }

    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = store
        _enabled.value = NotificationType.entries.associateWith {
            store.getBoolean(it.name, it.defaultEnabled)
        }
    }

    fun setEnabled(type: NotificationType, enabled: Boolean) {
        prefs?.edit()?.putBoolean(type.name, enabled)?.apply()
        _enabled.update { it + (type to enabled) }
    }

    /** Synchronous snapshot used by the notification delivery layer. */
    fun isEnabled(type: NotificationType): Boolean =
        prefs?.getBoolean(type.name, type.defaultEnabled) ?: type.defaultEnabled
}
