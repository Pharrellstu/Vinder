package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountFavoriteEntity(
    @SerialName("account_favorite_id") val favoriteId: Int,
    @SerialName("account_id") val accountId: Int,
    @SerialName("item_id") val itemId: Int,
)
