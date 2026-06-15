package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemCategoryEntity(
    @SerialName("item_category_id") val categoryId: Int,
    @SerialName("category_name") val name: String,
)
