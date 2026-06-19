package com.example.vinted.ui.models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AddProductUiState {
    object Idle : AddProductUiState()
    object Uploading : AddProductUiState()
    data class Submitted(val itemId: Int) : AddProductUiState()
    data class Error(val message: String) : AddProductUiState()
}

/**
 * Selectable category/condition chips for the Details step. Sourced from the DB so
 * they always map to existing item_category/item_condition rows — picking from a
 * hardcoded list could reference a name that was never seeded, failing the lookup.
 */
sealed class AddProductFormOptions {
    object Loading : AddProductFormOptions()
    data class Loaded(
        val categories: List<String>,
        val conditions: List<String>,
    ) : AddProductFormOptions()
    data class Error(val message: String) : AddProductFormOptions()
}

class AddProductViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddProductUiState>(AddProductUiState.Idle)
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    private val _formOptions = MutableStateFlow<AddProductFormOptions>(AddProductFormOptions.Loading)
    val formOptions: StateFlow<AddProductFormOptions> = _formOptions.asStateFlow()

    init {
        loadFormOptions()
    }

    private fun loadFormOptions() {
        viewModelScope.launch {
            _formOptions.value = AddProductFormOptions.Loading
            runCatching {
                val categories = repository.getCategoryNames()
                val conditions = repository.getConditionNames()
                AddProductFormOptions.Loaded(categories, conditions)
            }.onSuccess { _formOptions.value = it }
                .onFailure {
                    _formOptions.value = AddProductFormOptions.Error(
                        it.message ?: "Failed to load categories"
                    )
                }
        }
    }

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
        if (category.isBlank()) {
            _uiState.value = AddProductUiState.Error("Please choose a category")
            return
        }
        if (condition.isBlank()) {
            _uiState.value = AddProductUiState.Error("Please choose a condition")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddProductUiState.Uploading
            runCatching {
                val categoryId = repository.getCategoryId(category)
                val conditionId = repository.getConditionId(condition)
                // Insert with is_listed=false so incomplete listings are never visible
                val itemId = repository.insertItem(
                    sellerId = sellerId,
                    categoryId = categoryId,
                    conditionId = conditionId,
                    name = title,
                    description = description,
                    price = priceDouble,
                )
                // Read all photo bytes on IO thread before any uploads
                val photoBytesList = withContext(Dispatchers.IO) {
                    photoUris.map { uri ->
                        context.contentResolver.openInputStream(uri)?.readBytes()
                            ?: error("Failed to read photo: $uri")
                    }
                }
                photoBytesList.forEachIndexed { index, bytes ->
                    val url = repository.uploadPhoto(itemId, index, bytes)
                    repository.insertItemPhoto(itemId, url)
                }
                // All photos uploaded — flip listing to visible
                repository.updateItemToListed(itemId)
                itemId
            }.onSuccess { newItemId ->
                _uiState.value = AddProductUiState.Submitted(newItemId)
            }.onFailure {
                _uiState.value = AddProductUiState.Error(it.message ?: "Failed to post listing")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddProductUiState.Idle
    }
}
