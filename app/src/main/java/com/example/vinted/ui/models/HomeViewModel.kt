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

    // Server-filtered feed, accumulated across loadMore() pages. Every active filter (category,
    // search, price) is applied by getFeedItems in the DB, so this already holds only matching rows
    // across the whole catalog — not a client-side filter over a single loaded page.
    private var feedItems: List<Product> = emptyList()
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

    // Reloads from the first page with the current filters. Called on init and whenever a filter
    // changes, so the DB query — not an in-memory predicate — decides what the feed contains.
    fun load() {
        feedOffset = 0
        hasMore = true
        feedItems = emptyList()
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            runCatching {
                if (categories.isEmpty()) categories = repository.getCategories()
                val page = fetchPage(offset = 0)
                feedItems = page
                if (page.size < ItemRepository.PAGE_SIZE) hasMore = false
                feedOffset = page.size
            }.onSuccess { emitState() }
             .onFailure { _uiState.value = HomeUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load feed")) }
        }
    }

    fun loadMore() {
        if (!hasMore || _isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            runCatching {
                val page = fetchPage(offset = feedOffset)
                feedItems = feedItems + page
                if (page.size < ItemRepository.PAGE_SIZE) hasMore = false
                feedOffset += page.size
            }.onSuccess { emitState() }
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
        feedItems = feedItems.map { if (it.id == product.id) it.copy(isFavorite = newValue) else it }
        emitState()

        val itemId = product.id.toIntOrNull() ?: return
        val accountId = SessionManager.currentAccountId
        viewModelScope.launch {
            runCatching {
                if (newValue) repository.addFavorite(accountId, itemId) else repository.removeFavorite(accountId, itemId)
            }.onFailure {
                feedItems = feedItems.map { if (it.id == product.id) it.copy(isFavorite = !newValue) else it }
                emitState()
            }
        }
    }

    // Translates the active UI filters into a getFeedItems call so the DB applies them across the
    // whole catalog. PriceBucket bounds are inclusive-lower / exclusive-upper, matching
    // PriceBucket.matches; "All" category and a blank query are passed through as no-ops.
    private suspend fun fetchPage(offset: Int): List<Product> =
        repository.getFeedItems(
            searchQuery = searchQuery.ifBlank { null },
            category = selectedCategory,
            minPrice = selectedPriceBucket.minInclusive?.toDouble(),
            maxPrice = selectedPriceBucket.maxExclusive?.toDouble(),
            offset = offset,
        )

    // Publishes the already-filtered feed. The sale rail is just the discounted subset of what's
    // loaded, not a separate query.
    private fun emitState() {
        _uiState.value = HomeUiState.Success(
            categories = categories,
            saleItems = feedItems.filter { (it.discountPercent ?: 0) > 0 },
            gridItems = feedItems,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            selectedPriceBucket = selectedPriceBucket,
        )
    }
}
