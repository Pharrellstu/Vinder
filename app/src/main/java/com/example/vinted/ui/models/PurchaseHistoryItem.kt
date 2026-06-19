package com.example.vinted.ui.models

data class PurchaseHistoryItem(
    val purchaseId: Int,
    val itemName: String,
    val photoUrl: String?,
    val itemPrice: Double,
    val shippingFee: Double,
    val protectionFee: Double,
    val totalAmount: Double,
    val purchasedAt: String,
)
