package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseEntity(
    @SerialName("purchase_id") val purchaseId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("buyer_id") val buyerId: Int,
)
