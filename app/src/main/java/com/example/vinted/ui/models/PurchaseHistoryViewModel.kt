package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IPurchaseRepository
import com.example.vinted.data.PurchaseRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PurchaseHistoryUiState {
    object Loading : PurchaseHistoryUiState()
    data class Success(val items: List<PurchaseHistoryItem>) : PurchaseHistoryUiState()
    data class Error(val message: String) : PurchaseHistoryUiState()
}

class PurchaseHistoryViewModel(
    private val buyerId: Int? = null,
    private val repository: IPurchaseRepository = PurchaseRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<PurchaseHistoryUiState>(PurchaseHistoryUiState.Loading)
    val uiState: StateFlow<PurchaseHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val id = buyerId ?: SessionManager.currentAccountId
        if (id == SessionManager.NO_ACCOUNT_ID) {
            _uiState.value = PurchaseHistoryUiState.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _uiState.value = PurchaseHistoryUiState.Loading
            runCatching {
                PurchaseHistoryUiState.Success(repository.getBoughtItems(id))
            }.onSuccess { _uiState.value = it }
             .onFailure { _uiState.value = PurchaseHistoryUiState.Error(it.message ?: "Failed to load purchases") }
        }
    }
}
