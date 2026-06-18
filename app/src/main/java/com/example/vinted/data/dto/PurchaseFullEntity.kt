package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseFullEntity(
    @SerialName("purchase_id") val purchaseId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("buyer_id") val buyerId: Int,
    @SerialName("seller_id") val sellerId: Int,
    @SerialName("item_price") val itemPrice: Double,
    @SerialName("shipping_fee") val shippingFee: Double,
    @SerialName("protection_fee") val protectionFee: Double,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("created_at") val createdAt: String,
)
