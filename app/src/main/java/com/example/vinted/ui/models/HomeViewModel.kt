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

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val categories: List<String>,
        val saleItems: List<Product>,
        val gridItems: List<Product>,
        val favoritedIds: Set<Int> = emptySet(),
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            runCatching {
                val accountId = SessionManager.currentAccountId
                val categories = repository.getCategories()
                val allItems = repository.getFeedItems()
                val saleItems = allItems.filter { (it.discountPercent ?: 0) > 0 }
                val favoritedIds = if (accountId != SessionManager.NO_ACCOUNT_ID) {
                    runCatching { repository.getFavoriteItemIds(accountId) }.getOrDefault(emptySet())
                } else emptySet()
                HomeUiState.Success(categories, saleItems, allItems, favoritedIds)
            }.onSuccess { _uiState.value = it }
             .onFailure { _uiState.value = HomeUiState.Error(it.message ?: "Failed to load feed") }
        }
    }

    fun toggleFavorite(itemId: Int, currentlyFavorited: Boolean) {
        val current = _uiState.value as? HomeUiState.Success ?: return
        val accountId = SessionManager.currentAccountId
        if (accountId == SessionManager.NO_ACCOUNT_ID) return

        val optimisticIds = if (currentlyFavorited) {
            current.favoritedIds - itemId
        } else {
            current.favoritedIds + itemId
        }
        _uiState.value = current.copy(favoritedIds = optimisticIds)

        viewModelScope.launch {
            runCatching {
                if (currentlyFavorited) repository.removeFavorite(accountId, itemId)
                else repository.addFavorite(accountId, itemId)
            }.onFailure {
                _uiState.value = current.copy(favoritedIds = current.favoritedIds)
            }
        }
    }
}
