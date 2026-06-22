package com.example.vinted.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.components.FilterBottomSheet
import com.example.vinted.ui.components.SearchFilterBar
import com.example.vinted.ui.components.SearchResultsGrid
import com.example.vinted.ui.components.SearchResultsTopBar
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.models.SearchFilters
import com.example.vinted.ui.models.SearchResultsUiState
import com.example.vinted.ui.models.SearchResultsViewModel
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey95
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

private fun matchesFilters(product: Product, filters: SearchFilters): Boolean {
    val categoryMatch = filters.categories.isEmpty() || product.category in filters.categories
    val conditionMatch = filters.conditions.isEmpty() || product.condition in filters.conditions
    val sizeMatch = filters.sizes.isEmpty() || product.size in filters.sizes
    val priceMatch = product.price in filters.minPrice..filters.maxPrice
    return categoryMatch && conditionMatch && sizeMatch && priceMatch
}

private enum class SortOrder(val label: String) {
    NONE("Sort ↕"),
    PRICE_LOW_TO_HIGH("Price ↑"),
    PRICE_HIGH_TO_LOW("Price ↓"),
}

private fun SortOrder.next(): SortOrder = when (this) {
    SortOrder.NONE -> SortOrder.PRICE_LOW_TO_HIGH
    SortOrder.PRICE_LOW_TO_HIGH -> SortOrder.PRICE_HIGH_TO_LOW
    SortOrder.PRICE_HIGH_TO_LOW -> SortOrder.NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    onTabSelected: (Int) -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    viewModel: SearchResultsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var filters by remember { mutableStateOf(SearchFilters()) }
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NONE) }

    val items = (uiState as? SearchResultsUiState.Success)?.items ?: emptyList()
    val filteredResults = items.filter { matchesFilters(it, filters) }
    val resultCount = filteredResults.size
    val sortedResults = when (sortOrder) {
        SortOrder.NONE -> filteredResults
        SortOrder.PRICE_LOW_TO_HIGH -> filteredResults.sortedBy { it.price }
        SortOrder.PRICE_HIGH_TO_LOW -> filteredResults.sortedByDescending { it.price }
    }

    Scaffold(
        topBar = {
            Column {
                SearchResultsTopBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    onBack = { onTabSelected(0) },
                    onClear = {
                        searchQuery = ""
                        viewModel.search("")
                    },
                )
                SearchFilterBar(
                    sortLabel = sortOrder.label,
                    sortActive = sortOrder != SortOrder.NONE,
                    filterCount = filters.activeCount,
                    itemCount = resultCount,
                    onSortClick = { sortOrder = sortOrder.next() },
                    onFilterClick = { showFilters = true },
                )
            }
        },
        bottomBar = { BottomNavBar(selectedIndex = 1, onItemSelected = onTabSelected) },
        containerColor = Grey95,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is SearchResultsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VinderAzure)
                    }
                }
                is SearchResultsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Grey57, fontSize = 14.sp)
                    }
                }
                is SearchResultsUiState.Success -> {
                    SearchResultsGrid(
                        products = sortedResults,
                        modifier = Modifier.fillMaxSize(),
                        onProductClick = onProductClick,
                    )
                }
            }
        }
        if (showFilters) {
            ModalBottomSheet(
                onDismissRequest = { showFilters = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = null,
            ) {
                FilterBottomSheet(
                    filters = filters,
                    itemCount = resultCount,
                    onFiltersChange = { filters = it },
                    onReset = { filters = SearchFilters() },
                    onShowResults = { showFilters = false },
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchResultsScreenPreview() {
    VintedTheme {
        SearchResultsScreen()
    }
}
