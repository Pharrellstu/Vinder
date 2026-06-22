package com.example.vinted.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.IItemRepository
import com.example.vinted.data.ItemRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val CATEGORY_ALL = "All"

/**
 * Price buckets shown as filter chips. Bounds are inclusive of [minInclusive] and
 * exclusive of [maxExclusive]; a null bound means unbounded on that side.
 * [ANY] is the pass-through option (no price filtering).
 */
enum class PriceBucket(
    val label: String,
    val minInclusive: Float?,
    val maxExclusive: Float?,
) {
    ANY("Any price", null, null),
    UNDER_20("Under \$20", null, 20f),
    FROM_20_TO_50("\$20–\$50", 20f, 50f),
    OVER_50("\$50+", 50f, null);

    fun matches(price: Float): Boolean {
        if (minInclusive != null && price < minInclusive) return false
        if (maxExclusive != null && price >= maxExclusive) return false
        return true
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val categories: List<String>,
        val saleItems: List<Product>,
        val gridItems: List<Product>,
        val selectedCategory: String,
        val searchQuery: String,
        val selectedPriceBucket: PriceBucket,
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: IItemRepository = ItemRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Unfiltered feed kept in memory; accumulates pages across loadMore() calls.
    private var allItems: List<Product> = emptyList()
    private var categories: List<String> = emptyList()

    private var selectedCategory: String = CATEGORY_ALL
    private var searchQuery: String = ""
    private var selectedPriceBucket: PriceBucket = PriceBucket.ANY

    private var feedOffset = 0
    private var hasMore = true

    private companion object {
        const val TAG = "HomeViewModel"
    }

    init {
        load()
    }

    fun load() {
        feedOffset = 0
        hasMore = true
        allItems = emptyList()
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            runCatching {
                categories = repository.getCategories()
                val page = repository.getFeedItems(offset = 0)
                allItems = page
                if (page.size < ItemRepository.PAGE_SIZE) hasMore = false
                feedOffset = page.size
            }.onSuccess { emitFilteredState() }
             .onFailure { _uiState.value = HomeUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load feed")) }
        }
    }

    fun loadMore() {
        if (!hasMore || _isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            runCatching {
                val page = repository.getFeedItems(offset = feedOffset)
                allItems = allItems + page
                if (page.size < ItemRepository.PAGE_SIZE) hasMore = false
                feedOffset += page.size
            }.onSuccess { emitFilteredState() }
             .onFailure { Log.e(TAG, "loadMore failed — user can scroll again to retry", it) }
            _isLoadingMore.value = false
        }
    }

    fun onCategorySelected(category: String) {
        selectedCategory = category
        load()
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        load()
    }

    fun onPriceBucketSelected(bucket: PriceBucket) {
        selectedPriceBucket = bucket
        load()
    }

    fun onToggleFavorite(product: Product) {
        val newValue = !product.isFavorite
        allItems = allItems.map { if (it.id == product.id) it.copy(isFavorite = newValue) else it }
        emitFilteredState()

        val itemId = product.id.toIntOrNull() ?: return
        val accountId = SessionManager.currentAccountId
        viewModelScope.launch {
            runCatching {
                if (newValue) repository.addFavorite(accountId, itemId) else repository.removeFavorite(accountId, itemId)
            }.onFailure {
                allItems = allItems.map { if (it.id == product.id) it.copy(isFavorite = !newValue) else it }
                emitFilteredState()
            }
        }
    }

    /**
     * Applies every active filter together (logical AND) to the cached feed and
     * publishes a fresh Success state. Each filter is a pass-through when unset:
     * category "All", blank search query, and [PriceBucket.ANY].
     */
    private fun emitFilteredState() {
        val filtered = allItems.filter { product ->
            matchesCategory(product) && matchesSearch(product) && matchesPrice(product)
        }
        _uiState.value = HomeUiState.Success(
            categories = categories,
            saleItems = filtered.filter { (it.discountPercent ?: 0) > 0 },
            gridItems = filtered,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            selectedPriceBucket = selectedPriceBucket,
        )
    }

    private fun matchesCategory(product: Product): Boolean {
        if (selectedCategory == CATEGORY_ALL) return true
        return product.category == selectedCategory
    }

    private fun matchesSearch(product: Product): Boolean {
        if (searchQuery.isBlank()) return true
        val words = product.name.split(" ")
        return words.indices.any { start ->
            words.subList(start, words.size).joinToString(" ").startsWith(searchQuery, ignoreCase = true)
        }
    }

    private fun matchesPrice(product: Product): Boolean {
        return selectedPriceBucket.matches(product.price)
    }
}
