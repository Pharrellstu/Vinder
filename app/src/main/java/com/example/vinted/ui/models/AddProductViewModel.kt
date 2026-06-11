package com.example.vinted.ui.models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddProductUiState {
    object Idle : AddProductUiState()
    object Uploading : AddProductUiState()
    object Submitted : AddProductUiState()
    data class Error(val message: String) : AddProductUiState()
}

class AddProductViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddProductUiState>(AddProductUiState.Idle)
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    fun postListing(
        context: Context,
        photoUris: List<Uri>,
        title: String,
        description: String,
        price: String,
        category: String,
        condition: String,
    ) {
        val sellerId = SessionManager.currentAccountId
        if (sellerId == -1) {
            _uiState.value = AddProductUiState.Error("Not logged in")
            return
        }
        val priceDouble = price.toDoubleOrNull()
        if (priceDouble == null || priceDouble <= 0) {
            _uiState.value = AddProductUiState.Error("Invalid price")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddProductUiState.Uploading
            runCatching {
                val categoryId = repository.getCategoryId(category)
                val conditionId = repository.getConditionId(condition)
                val itemId = repository.insertItem(
                    sellerId = sellerId,
                    categoryId = categoryId,
                    conditionId = conditionId,
                    name = title,
                    description = description,
                    price = priceDouble,
                )
                photoUris.forEachIndexed { index, uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val url = repository.uploadPhoto(itemId, index, bytes)
                        repository.insertItemPhoto(itemId, url)
                    }
                }
            }.onSuccess {
                _uiState.value = AddProductUiState.Submitted
            }.onFailure {
                _uiState.value = AddProductUiState.Error(it.message ?: "Failed to post listing")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddProductUiState.Idle
    }
}
