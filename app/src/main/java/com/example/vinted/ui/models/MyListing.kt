package com.example.vinted.ui.models

/** A seller's own item as shown on the My Listings management screen. */
data class MyListing(
    val itemId: Int,
    val name: String,
    val price: Int,
    val coverUrl: String?,
    val isSold: Boolean,
)
