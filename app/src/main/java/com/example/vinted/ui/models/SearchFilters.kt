package com.example.vinted.ui.models

data class SearchFilters(
    val categories: Set<String> = emptySet(),
    val conditions: Set<String> = emptySet(),
    val sizes: Set<String> = emptySet(),
    val minPrice: Float = 0f,
    val maxPrice: Float = 500f,
) {
    val activeCount: Int
        get() = categories.size + conditions.size + sizes.size
}
