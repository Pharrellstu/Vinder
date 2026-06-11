package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemConditionEntity(
    @SerialName("item_condition_id") val conditionId: Int,
    @SerialName("item_condition_name") val name: String,
)
