package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemPhotoEntity(
    @SerialName("item_photo_id") val itemPhotoId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("photo_url") val photoUrl: String,
)
