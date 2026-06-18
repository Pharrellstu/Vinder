package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
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
        viewModelScope.launch {
            _uiState.value = WishlistUiState.Loading
            runCatching {
                repository.getFavoriteItems(SessionManager.currentAccountId)
            }.onSuccess { _uiState.value = WishlistUiState.Success(it) }
             .onFailure { _uiState.value = WishlistUiState.Error(it.message ?: "Failed to load wishlist") }
        }
    }

    fun removeFromWishlist(product: Product) {
        val itemId = product.id.toIntOrNull() ?: return
        viewModelScope.launch {
            runCatching {
                repository.removeFavorite(SessionManager.currentAccountId, itemId)
            }.onSuccess { load() }
        }
    }
}
