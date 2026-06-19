package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IPurchaseRepository
import com.example.vinted.data.PurchaseRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseWithItem(
    val purchaseId: Int,
    val itemId: Int,
    val itemName: String,
    val itemPrice: Double,
    val shippingFee: Double,
    val protectionFee: Double,
    val totalAmount: Double,
    val createdAt: String,
)

sealed class OrderHistoryUiState {
    object Loading : OrderHistoryUiState()
    data class Success(val orders: List<PurchaseWithItem>) : OrderHistoryUiState()
    data class Error(val message: String) : OrderHistoryUiState()
}

class OrderHistoryViewModel(
    private val purchaseRepo: IPurchaseRepository = PurchaseRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderHistoryUiState>(OrderHistoryUiState.Loading)
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = OrderHistoryUiState.Loading
            runCatching {
                purchaseRepo.getMyPurchases(SessionManager.currentAccountId)
            }.onSuccess { _uiState.value = OrderHistoryUiState.Success(it) }
             .onFailure { _uiState.value = OrderHistoryUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load orders")) }
        }
    }
}
