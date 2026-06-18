package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.NotificationPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A single switchable notification preference. */
enum class NotificationType(
    val group: String,
    val title: String,
    val description: String,
    val defaultEnabled: Boolean,
) {
    NEW_MESSAGES("Messages", "New messages", "When a buyer or seller messages you", true),
    MESSAGE_REQUESTS("Messages", "Message requests", "When someone new starts a chat", true),

    OFFERS("Activity", "Offers", "When someone makes an offer on your item", true),
    ITEM_SOLD("Activity", "Item sold", "When one of your items sells", true),
    NEW_FOLLOWERS("Activity", "New followers", "When someone follows your profile", false),
    REVIEWS("Activity", "Reviews", "When you receive a new review", true),

    PRICE_DROPS("Marketing", "Price drops", "When items on your wishlist drop in price", false),
    PROMOTIONS("Marketing", "Promotions & deals", "Discounts and seasonal offers", false),
    PRODUCT_UPDATES("Marketing", "Vinder updates", "News about new app features", false),
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
