package com.example.vinted.ui.models

// Top of the price slider. Sitting at this value means "and up" (the slider shows "€500+"), i.e. no
// upper bound — so items priced above it must still match. Shared by the slider, its label, and the
// price-filter predicate so all three agree on the ceiling.
const val PRICE_FILTER_MAX = 500f

data class SearchFilters(
    val categories: Set<String> = emptySet(),
    val conditions: Set<String> = emptySet(),
    val sizes: Set<String> = emptySet(),
    val minPrice: Float = 0f,
    val maxPrice: Float = PRICE_FILTER_MAX,
) {
    val activeCount: Int
        get() = categories.size + conditions.size + sizes.size

    // A price bound only restricts once the user moves it off the slider ends; at the ends it means
    // "no bound" so items priced above PRICE_FILTER_MAX (e.g. €800) are not silently dropped.
    fun matchesPrice(price: Float): Boolean {
        if (price < minPrice) return false
        if (maxPrice < PRICE_FILTER_MAX && price > maxPrice) return false
        return true
    }
}
