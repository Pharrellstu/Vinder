package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
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
                val categories = repository.getCategories()
                val allItems = repository.getFeedItems()
                val saleItems = allItems.filter { (it.discountPercent ?: 0) > 0 }
                val gridItems = allItems.filter { (it.discountPercent ?: 0) == 0 }
                HomeUiState.Success(categories, saleItems, gridItems)
            }.onSuccess { _uiState.value = it }
             .onFailure { _uiState.value = HomeUiState.Error(it.message ?: "Failed to load feed") }
        }
    }
}
