package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    fun setEnabled(type: NotificationType, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(enabled = state.enabled + (type to enabled))
        }
    }
}
