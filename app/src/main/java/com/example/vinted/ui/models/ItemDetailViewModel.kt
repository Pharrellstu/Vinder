package com.example.vinted.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.AccountRepository
import com.example.vinted.data.IAccountRepository
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.IPurchaseRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.PurchaseRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ItemDetailUiState {
    object Loading : ItemDetailUiState()
    data class Success(
        val photoUrls: List<String>,
        val description: String,
        val seller: Seller,
    ) : ItemDetailUiState()
    data class Error(val message: String) : ItemDetailUiState()
}

class ItemDetailViewModel(
    private val itemId: Int,
    private val sellerId: Int,
    private val itemRepo: IItemRepository = ItemRepository(),
    private val accountRepo: IAccountRepository = AccountRepository(),
    private val purchaseRepo: IPurchaseRepository = PurchaseRepository(),
) : ViewModel() {

    companion object {
        const val SHIPPING_FEE = 3.95
        const val BUYER_PROTECTION_FEE = 0.90
    }

    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    var isFavorited by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun confirmBuy(
        sellerId: Int,
        itemPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val buyerId = SessionManager.currentAccountId
        if (sellerId == buyerId) {
            onError("You can't buy your own listing.")
            return
        }
        viewModelScope.launch {
            runCatching {
                purchaseRepo.createPurchase(itemId, buyerId, sellerId, itemPrice, SHIPPING_FEE, BUYER_PROTECTION_FEE)
                itemRepo.markAsSold(itemId)
            }.onSuccess { onSuccess() }
             .onFailure { onError(it.message ?: "Purchase failed") }
        }
    }

    fun submitOffer(
        offerPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val creatorId = SessionManager.currentAccountId
        if (sellerId == creatorId) {
            onError("You can't make an offer on your own listing.")
            return
        }
        viewModelScope.launch {
            runCatching { itemRepo.createOffer(itemId, creatorId, offerPrice) }
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Offer failed") }
        }
    }

    fun toggleFavorite(onMessage: (String) -> Unit) {
        val accountId = SessionManager.currentAccountId
        if (accountId == SessionManager.NO_ACCOUNT_ID) return
        val wasLiked = isFavorited
        isFavorited = !wasLiked
        viewModelScope.launch {
            runCatching {
                if (wasLiked) itemRepo.removeFavorite(accountId, itemId)
                else itemRepo.addFavorite(accountId, itemId)
            }.onSuccess {
                onMessage(if (!wasLiked) "Saved to wishlist" else "Removed from wishlist")
            }.onFailure {
                isFavorited = wasLiked
                onMessage("Couldn't update wishlist")
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                val accountId = SessionManager.currentAccountId
                val photosDeferred = async { itemRepo.getItemPhotos(itemId) }
                val descDeferred = async { itemRepo.getItemDescription(itemId) }
                val profileDeferred = async { accountRepo.getProfile(sellerId) }
                val favDeferred = async {
                    if (accountId != SessionManager.NO_ACCOUNT_ID) {
                        itemRepo.getFavoriteItemIds(accountId).contains(itemId)
                    } else false
                }
                val photos = photosDeferred.await()
                val desc = descDeferred.await()
                val profile = profileDeferred.await()
                isFavorited = favDeferred.await()
                Triple(photos, desc, profile)
            }.onSuccess { (photos, desc, profile) ->
                _uiState.value = ItemDetailUiState.Success(
                    photoUrls = photos,
                    description = desc,
                    seller = profile.toSeller(),
                )
            }.onFailure {
                _uiState.value = ItemDetailUiState.Error(it.message ?: "Failed to load item")
            }
        }
    }
}

private fun UserProfile.toSeller() = Seller(
    initial = this.initial,
    name = this.handle,
    rating = this.rating,
    reviewCount = this.reviewCount,
    itemCount = this.listedCount,
    location = this.location,
    memberSince = "-",
    isVerified = this.rating >= 4.8f,
)

class ItemDetailViewModelFactory(
    private val itemId: Int,
    private val sellerId: Int,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ItemDetailViewModel(itemId, sellerId) as T
}
