package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchResultsUiState {
    object Loading : SearchResultsUiState()
    data class Success(val items: List<Product>) : SearchResultsUiState()
    data class Error(val message: String) : SearchResultsUiState()
}

class SearchResultsViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchResultsUiState>(SearchResultsUiState.Loading)
    val uiState: StateFlow<SearchResultsUiState> = _uiState.asStateFlow()

    init {
        search("")
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchResultsUiState.Loading
            runCatching {
                repository.getFeedItems(query.ifBlank { null })
            }.onSuccess { _uiState.value = SearchResultsUiState.Success(it) }
             .onFailure { _uiState.value = SearchResultsUiState.Error(it.message ?: "Search failed") }
        }
    }
}
