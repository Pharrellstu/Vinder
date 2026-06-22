package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.NotificationPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * A single switchable notification preference. Only notifications the app can actually deliver with
 * the current backend (Supabase Realtime on tables in the `supabase_realtime` publication) are
 * listed — each entry is backed by a real Realtime listener in MessageNotificationController.
 * Options without a delivery mechanism (message requests, reviews, and the marketing pushes) were
 * intentionally removed rather than shown as toggles that do nothing.
 */
enum class NotificationType(
    val group: String,
    val title: String,
    val description: String,
    val defaultEnabled: Boolean,
) {
    NEW_MESSAGES("Messages", "New messages", "When a buyer or seller messages you", true),

    OFFERS("Activity", "Offers", "When someone makes an offer on your item", true),
    ITEM_SOLD("Activity", "Item sold", "When one of your items sells", true),
    NEW_FOLLOWERS("Activity", "New followers", "When someone follows your profile", false),
}

data class NotificationSettingsUiState(
    val enabled: Map<NotificationType, Boolean> =
        NotificationType.entries.associateWith { it.defaultEnabled },
)

class NotificationSettingsViewModel : ViewModel() {

    val uiState: StateFlow<NotificationSettingsUiState> =
        NotificationPreferences.enabled
            .map { NotificationSettingsUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NotificationSettingsUiState(NotificationPreferences.enabled.value),
            )

    fun setEnabled(type: NotificationType, enabled: Boolean) {
        NotificationPreferences.setEnabled(type, enabled)
    }
}
