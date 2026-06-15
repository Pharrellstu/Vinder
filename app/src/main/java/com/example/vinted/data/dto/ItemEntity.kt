package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemEntity(
    @SerialName("item_id") val itemId: Int,
    @SerialName("seller_id") val sellerId: Int,
    @SerialName("item_category_id") val categoryId: Int,
    @SerialName("item_condition_id") val conditionId: Int,
    @SerialName("item_name") val name: String,
    @SerialName("item_description") val description: String,
    @SerialName("item_price") val price: Double,
    @SerialName("item_discount") val discount: Int? = null,
    @SerialName("is_listed") val isListed: Boolean,
    @SerialName("is_sold") val isSold: Boolean,
    @SerialName("created_at") val createdAt: String,
)
