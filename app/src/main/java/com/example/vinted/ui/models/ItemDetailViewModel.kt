package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.AccountRepository
import com.example.vinted.data.IAccountRepository
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                val photosDeferred = async { itemRepo.getItemPhotos(itemId) }
                val descDeferred = async { itemRepo.getItemDescription(itemId) }
                val profileDeferred = async { accountRepo.getProfile(sellerId) }
                Triple(photosDeferred.await(), descDeferred.await(), profileDeferred.await())
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
