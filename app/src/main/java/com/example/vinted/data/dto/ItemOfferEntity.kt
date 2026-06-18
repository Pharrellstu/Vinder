package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemOfferEntity(
    @SerialName("item_offer_id") val itemOfferId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("offer_creator_id") val offerCreatorId: Int,
    @SerialName("offer_status_id") val offerStatusId: Int,
    @SerialName("offer_price") val offerPrice: Double,
    @SerialName("created_at") val createdAt: String,
)
