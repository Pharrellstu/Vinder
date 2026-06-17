package com.example.vinted.data

import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import io.github.jan.supabase.postgrest.from

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
            mapOf(
                "item_id" to itemId,
                "buyer_id" to buyerId,
                "seller_id" to sellerId,
                "item_price" to itemPrice,
                "shipping_fee" to shippingFee,
                "protection_fee" to protectionFee,
                "total_amount" to (itemPrice + shippingFee + protectionFee),
            )
        )
    }
}
