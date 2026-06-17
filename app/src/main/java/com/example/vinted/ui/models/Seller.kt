package com.example.vinted.ui.models

data class Seller(
    val initial: String,
    val name: String,
    val rating: Float,
    val reviewCount: Int,
    val itemCount: Int,
    val location: String,
    val memberSince: String,
    val isVerified: Boolean = false,
)
