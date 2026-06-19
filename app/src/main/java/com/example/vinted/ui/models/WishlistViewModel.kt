package com.example.vinted.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.launch

sealed class WishlistUiState {
    object Loading : WishlistUiState()
    data class Success(val items: List<Product>) : WishlistUiState()
    data class Error(val message: String) : WishlistUiState()
}

class WishlistViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    var uiState by mutableStateOf<WishlistUiState>(WishlistUiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        val accountId = SessionManager.currentAccountId
        if (accountId == SessionManager.NO_ACCOUNT_ID) {
            uiState = WishlistUiState.Success(emptyList())
            return
        }
        uiState = WishlistUiState.Loading
        viewModelScope.launch {
            runCatching { repository.getFavoriteItems(accountId) }
                .onSuccess { uiState = WishlistUiState.Success(it) }
                .onFailure { uiState = WishlistUiState.Error(it.message ?: "Failed to load wishlist") }
        }
    }

    fun removeItem(itemId: Int) {
        val accountId = SessionManager.currentAccountId
        if (accountId == SessionManager.NO_ACCOUNT_ID) return
        val current = uiState as? WishlistUiState.Success ?: return
        val optimistic = current.items.filter { it.id.toIntOrNull() != itemId }
        uiState = WishlistUiState.Success(optimistic)
        viewModelScope.launch {
            runCatching { repository.removeFavorite(accountId, itemId) }
                .onFailure { uiState = current }
        }
    }
}
