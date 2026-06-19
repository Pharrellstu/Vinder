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

sealed class MyListingsUiState {
    object Loading : MyListingsUiState()
    data class Success(val listings: List<MyListing>) : MyListingsUiState()
    data class Error(val message: String) : MyListingsUiState()
}

class MyListingsViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyListingsUiState>(MyListingsUiState.Loading)
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    // One-shot message surfaced when an action fails (e.g. deleting a purchased item).
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MyListingsUiState.Loading
            runCatching {
                repository.getMyListings(SessionManager.currentAccountId)
            }.onSuccess { _uiState.value = MyListingsUiState.Success(it) }
             .onFailure { _uiState.value = MyListingsUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load listings")) }
        }
    }

    fun markSold(itemId: Int) {
        viewModelScope.launch {
            runCatching { repository.markAsSold(itemId) }
                .onSuccess { load() }
                .onFailure { _actionError.value = ErrorMessages.friendlyMessage(it, "Couldn't mark this item as sold") }
        }
    }

    fun delete(itemId: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteItem(itemId) }
                .onSuccess { load() }
                .onFailure {
                    _actionError.value =
                        "Couldn't delete this listing. Items that have been purchased can't be removed."
                }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
