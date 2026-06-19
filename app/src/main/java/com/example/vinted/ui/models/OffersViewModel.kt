package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.IPurchaseRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.OFFER_STATUS_ACCEPTED
import com.example.vinted.data.OFFER_STATUS_PENDING
import com.example.vinted.data.OFFER_STATUS_REJECTED
import com.example.vinted.data.PurchaseRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfferWithDetails(
    val offerId: Int,
    val itemId: Int,
    val itemName: String,
    val buyerId: Int,
    val buyerName: String,
    val offerPrice: Double,
    val statusId: Int,
    val createdAt: String,
)

sealed class OffersUiState {
    object Loading : OffersUiState()
    data class Success(val offers: List<OfferWithDetails>) : OffersUiState()
    data class Error(val message: String) : OffersUiState()
}

class OffersViewModel(
    private val itemRepo: IItemRepository = ItemRepository(),
    private val purchaseRepo: IPurchaseRepository = PurchaseRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<OffersUiState>(OffersUiState.Loading)
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = OffersUiState.Loading
            runCatching {
                itemRepo.getOffersForSeller(SessionManager.currentAccountId)
            }.onSuccess { _uiState.value = OffersUiState.Success(it) }
             .onFailure { _uiState.value = OffersUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load offers")) }
        }
    }

    fun acceptOffer(
        offer: OfferWithDetails,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                itemRepo.updateOfferStatus(offer.offerId, OFFER_STATUS_ACCEPTED)
                purchaseRepo.createPurchaseFromOffer(
                    itemId = offer.itemId,
                    buyerId = offer.buyerId,
                    sellerId = SessionManager.currentAccountId,
                    offerPrice = offer.offerPrice,
                )
                itemRepo.markAsSold(offer.itemId)
            }.onSuccess {
                onSuccess()
                load()
            }.onFailure { onError(ErrorMessages.friendlyMessage(it, "Failed to accept offer")) }
        }
    }

    fun rejectOffer(
        offerId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                itemRepo.updateOfferStatus(offerId, OFFER_STATUS_REJECTED)
            }.onSuccess {
                onSuccess()
                load()
            }.onFailure { onError(ErrorMessages.friendlyMessage(it, "Failed to reject offer")) }
        }
    }
}
