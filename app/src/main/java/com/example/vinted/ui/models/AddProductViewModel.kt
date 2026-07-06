package com.example.vinted.ui.models

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.data.dto.ItemPhotoEntity
import com.example.vinted.util.ErrorMessages
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

    // Existing listing being edited; null in create mode. Drives the form prefill.
    private val _editData = MutableStateFlow<EditableItem?>(null)
    val editData: StateFlow<EditableItem?> = _editData.asStateFlow()

    init {
        loadFormOptions()
    }

    fun loadForEdit(itemId: Int) {
        if (_editData.value?.itemId == itemId) return
        // Drop any previously loaded item so the form never prefills stale data while
        // the new one is being fetched.
        _editData.value = null
        viewModelScope.launch {
            runCatching { repository.getItemForEdit(itemId) }
                .onSuccess { _editData.value = it }
                .onFailure { _uiState.value = AddProductUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load listing")) }
        }
    }

    private fun loadFormOptions() {
        viewModelScope.launch {
            _formOptions.value = AddProductFormOptions.Loading
            runCatching {
                AddProductFormOptions.Loaded(
                    categories = repository.getCategoryNames(),
                    conditions = repository.getConditionNames(),
                )
            }.onSuccess { _formOptions.value = it }
             .onFailure {
                _formOptions.value = AddProductFormOptions.Error(
                    ErrorMessages.friendlyMessage(it, "Failed to load categories")
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
        if (sellerId == -1) { _uiState.value = AddProductUiState.Error("Not logged in"); return }
        val inputError = validateListingInputs(price, category, condition)
        if (inputError != null) { _uiState.value = AddProductUiState.Error(inputError); return }
        val priceDouble = price.toDouble()
        viewModelScope.launch {
            _uiState.value = AddProductUiState.Uploading
            runCatching { createListing(context, sellerId, title, description, priceDouble, category, condition, photoUris) }
                .onSuccess { _uiState.value = AddProductUiState.Submitted(it) }
                .onFailure { _uiState.value = AddProductUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to post listing")) }
        }
    }

    fun updateListing(
        context: Context,
        itemId: Int,
        title: String,
        description: String,
        price: String,
        category: String,
        condition: String,
        removedPhotos: List<ItemPhotoEntity>,
        newPhotoUris: List<Uri>,
    ) {
        val inputError = validateListingInputs(price, category, condition)
        if (inputError != null) { _uiState.value = AddProductUiState.Error(inputError); return }
        val priceDouble = price.toDouble()
        viewModelScope.launch {
            _uiState.value = AddProductUiState.Uploading
            runCatching { applyUpdate(context, itemId, title, description, priceDouble, category, condition, removedPhotos, newPhotoUris) }
                .onSuccess { _uiState.value = AddProductUiState.Submitted(it) }
                .onFailure { _uiState.value = AddProductUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to update listing")) }
        }
    }

    fun resetState() {
        _uiState.value = AddProductUiState.Idle
    }

    private fun validateListingInputs(price: String, category: String, condition: String): String? {
        val priceDouble = price.toDoubleOrNull()
        return when {
            priceDouble == null || priceDouble <= 0 -> "Invalid price"
            category.isBlank() -> "Please choose a category"
            condition.isBlank() -> "Please choose a condition"
            else -> null
        }
    }

    private suspend fun readPhotoBytes(context: Context, uris: List<Uri>): List<ByteArray> =
        withContext(Dispatchers.IO) {
            uris.map { uri ->
                context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: error("Failed to read photo: $uri")
            }
        }

    private suspend fun createListing(
        context: Context,
        sellerId: Int,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        photoUris: List<Uri>,
    ): Int {
        val categoryId = repository.getCategoryId(category)
        val conditionId = repository.getConditionId(condition)
        // Insert with is_listed=false so incomplete listings are never visible.
        val itemId = repository.insertItem(
            sellerId = sellerId,
            categoryId = categoryId,
            conditionId = conditionId,
            name = title,
            description = description,
            price = price,
        )
        readPhotoBytes(context, photoUris).forEachIndexed { index, bytes ->
            repository.insertItemPhoto(itemId, repository.uploadPhoto(itemId, index, bytes))
        }
        repository.updateItemToListed(itemId)
        return itemId
    }

    private suspend fun applyUpdate(
        context: Context,
        itemId: Int,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        removedPhotos: List<ItemPhotoEntity>,
        newPhotoUris: List<Uri>,
    ): Int {
        repository.updateItem(
            itemId = itemId,
            name = title,
            description = description,
            price = price,
            categoryId = repository.getCategoryId(category),
            conditionId = repository.getConditionId(condition),
        )
        removedPhotos.forEach { repository.deleteItemPhoto(it.itemPhotoId, it.photoUrl) }
        readPhotoBytes(context, newPhotoUris).forEach { bytes ->
            repository.insertItemPhoto(itemId, repository.uploadPhotoUnique(itemId, bytes))
        }
        return itemId
    }
}
