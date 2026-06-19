package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WishlistUiState {
    object Loading : WishlistUiState()
    data class Success(val items: List<Product>) : WishlistUiState()
    data class Error(val message: String) : WishlistUiState()
}

class WishlistViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val accountId = SessionManager.currentAccountId
        if (accountId == SessionManager.NO_ACCOUNT_ID) {
            _uiState.value = WishlistUiState.Success(emptyList())
            return
        }
        _uiState.value = WishlistUiState.Loading
        viewModelScope.launch {
            runCatching { repository.getFavoriteItems(accountId) }
                .onSuccess { _uiState.value = WishlistUiState.Success(it) }
                .onFailure { _uiState.value = WishlistUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load wishlist")) }
        }
    }

    fun removeFromWishlist(product: Product) {
        val accountId = SessionManager.currentAccountId
        if (accountId == SessionManager.NO_ACCOUNT_ID) return
        val current = _uiState.value as? WishlistUiState.Success ?: return
        val optimistic = current.items.filter { it.id != product.id }
        _uiState.value = WishlistUiState.Success(optimistic)
        viewModelScope.launch {
            val itemId = product.id.toIntOrNull() ?: return@launch
            runCatching { repository.removeFavorite(accountId, itemId) }
                .onFailure { _uiState.value = current }
        }
    }
}
