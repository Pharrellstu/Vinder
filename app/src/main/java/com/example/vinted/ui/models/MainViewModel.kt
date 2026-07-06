package com.example.vinted.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.DialogueRepository
import com.example.vinted.data.InboxBadge
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.launch

sealed interface MainOverlay {
    object AddProduct : MainOverlay
    object EditProfile : MainOverlay
    object Offers : MainOverlay
    object OrderHistory : MainOverlay
    object Wishlist : MainOverlay
    data class EditItem(val itemId: Int) : MainOverlay
    object MyListings : MainOverlay
    object Notifications : MainOverlay
    object ChangePassword : MainOverlay
    object Settings : MainOverlay
    data class Chat(val target: ChatTarget) : MainOverlay
    data class Seller(val accountId: Int) : MainOverlay
    data class ProductDetail(val product: Product) : MainOverlay
}

data class ChatTarget(val sellerId: Int, val itemId: Int?)

class MainViewModel : ViewModel() {

    val dialogueRepository = DialogueRepository()

    var selectedTab by mutableStateOf(0)
        private set

    var overlay by mutableStateOf<MainOverlay?>(null)
        private set

    // Incremented rather than toggled so ProfileScreen fully recomposes (via `key`) after each edit.
    var profileReloadToken by mutableStateOf(0)
        private set

    init {
        refreshInboxBadge()
    }

    fun selectTab(index: Int) {
        selectedTab = index
    }

    fun selectTabFab() {
        overlay = MainOverlay.AddProduct
    }

    fun showOverlay(overlay: MainOverlay) {
        this.overlay = overlay
    }

    fun clearOverlay() {
        overlay = null
    }

    fun reloadProfile() {
        profileReloadToken++
    }

    fun loggedOut(onLoggedOut: () -> Unit) {
        overlay = null
        selectedTab = 0
        onLoggedOut()
    }

    private fun refreshInboxBadge() {
        val accountId = SessionManager.currentAccountId
        if (accountId == -1) return
        viewModelScope.launch {
            runCatching { InboxBadge.unread.value = dialogueRepository.getUnreadCount(accountId) }
        }
    }
}
