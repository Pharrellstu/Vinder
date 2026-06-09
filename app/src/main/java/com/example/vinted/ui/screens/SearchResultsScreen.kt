package com.example.vinted.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.components.FilterBottomSheet
import com.example.vinted.ui.components.SearchFilterBar
import com.example.vinted.ui.components.SearchResultsGrid
import com.example.vinted.ui.components.SearchResultsTopBar
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.models.SearchFilters
import com.example.vinted.ui.theme.VintedTheme

private val screenBgColor = Color(0xFFF0F0F5)

private val sampleSearchResults = listOf(
    Product("r1", "Linen midi dress", 28f, 45f, 38, "M", "& Other Stories", "L", "lena.k", 4.9f),
    Product("r2", "Linen wrap dress", 32f, null, null, "S", "Mango", "S", "sophie.b", 4.6f),
    Product("r3", "Linen shirt dress", 38f, 60f, 36, "M", "COS", "M", "maya.r", 5.0f),
    Product("r4", "Linen tunic", 24f, null, null, "L", "Uniqlo", "N", "noah.dev", 4.8f),
    Product("r5", "Linen sundress", 42f, 75f, 44, "S", "Aritzia", "C", "chloe.p", 4.9f),
    Product("r6", "Linen maxi dress", 55f, null, null, "M", "Reformation", "E", "emma.w", 5.0f),
)

@Composable
fun SearchResultsScreen(onTabSelected: (Int) -> Unit = {}) {
    var filters by remember {
        mutableStateOf(
            SearchFilters(
                categories = setOf("Women", "Kids"),
                conditions = setOf("Like new", "Good"),
            ),
        )
    }
    var showFilters by remember { mutableStateOf(true) }
    val resultCount = 142

    Scaffold(
        topBar = {
            Column {
                SearchResultsTopBar(query = "linen dress", onBack = {}, onClear = {})
                SearchFilterBar(
                    filterCount = filters.activeCount,
                    itemCount = resultCount,
                    onSortClick = {},
                    onFilterClick = { showFilters = true },
                )
            }
        },
        bottomBar = { BottomNavBar(selectedIndex = 1, onItemSelected = onTabSelected) },
        containerColor = screenBgColor,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchResultsGrid(
                products = sampleSearchResults,
                modifier = Modifier.fillMaxSize(),
            )
            if (showFilters) {
                FilterBottomSheet(
                    filters = filters,
                    itemCount = resultCount,
                    onFiltersChange = { filters = it },
                    onReset = { filters = SearchFilters() },
                    onShowResults = { showFilters = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
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
