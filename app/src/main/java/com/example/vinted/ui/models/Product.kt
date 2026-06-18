package com.example.vinted.ui.models

data class Product(
    val id: String,
    val name: String,
    val price: Float,
    val originalPrice: Float? = null,
    val discountPercent: Int? = null,
    val size: String? = null,
    val brand: String? = null,
    val sellerInitial: String,
    val sellerName: String,
    val rating: Float,
    val sellerId: Int = -1,
)
