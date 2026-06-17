package com.example.vinted.data

import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class PurchaseInsert(
    @SerialName("item_id") val itemId: Int,
    @SerialName("buyer_id") val buyerId: Int,
    @SerialName("seller_id") val sellerId: Int,
    @SerialName("item_price") val itemPrice: Double,
    @SerialName("shipping_fee") val shippingFee: Double,
    @SerialName("protection_fee") val protectionFee: Double,
    @SerialName("total_amount") val totalAmount: Double,
)

interface IPurchaseRepository {
    suspend fun createPurchase(
        itemId: Int,
        buyerId: Int,
        sellerId: Int,
        itemPrice: Double,
        shippingFee: Double,
        protectionFee: Double,
    )
}

class PurchaseRepository : IPurchaseRepository {

    private val client = SupabaseClientInitialiser.client

    override suspend fun createPurchase(
        itemId: Int,
        buyerId: Int,
        sellerId: Int,
        itemPrice: Double,
        shippingFee: Double,
        protectionFee: Double,
    ) {
        client.from("purchase").insert(
            PurchaseInsert(
                itemId = itemId,
                buyerId = buyerId,
                sellerId = sellerId,
                itemPrice = itemPrice,
                shippingFee = shippingFee,
                protectionFee = protectionFee,
                totalAmount = itemPrice + shippingFee + protectionFee,
            )
        )
    }
}
