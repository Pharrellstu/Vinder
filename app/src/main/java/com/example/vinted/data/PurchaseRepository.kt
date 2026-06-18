package com.example.vinted.data

import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.data.dto.PurchaseFullEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.PurchaseWithItem
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
    suspend fun createPurchaseFromOffer(
        itemId: Int,
        buyerId: Int,
        sellerId: Int,
        offerPrice: Double,
    )
    suspend fun getMyPurchases(buyerId: Int): List<PurchaseWithItem>
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

    override suspend fun createPurchaseFromOffer(
        itemId: Int,
        buyerId: Int,
        sellerId: Int,
        offerPrice: Double,
    ) {
        client.from("purchase").insert(
            PurchaseInsert(
                itemId = itemId,
                buyerId = buyerId,
                sellerId = sellerId,
                itemPrice = offerPrice,
                shippingFee = SHIPPING_FEE,
                protectionFee = PROTECTION_FEE,
                totalAmount = offerPrice + SHIPPING_FEE + PROTECTION_FEE,
            )
        )
    }

    override suspend fun getMyPurchases(buyerId: Int): List<PurchaseWithItem> {
        val purchases = client.from("purchase")
            .select { filter { eq("buyer_id", buyerId) } }
            .decodeList<PurchaseFullEntity>()

        if (purchases.isEmpty()) return emptyList()

        val itemIds = purchases.map { it.itemId }.distinct()
        val itemNameMap = client.from("item")
            .select { filter { isIn("item_id", itemIds) } }
            .decodeList<ItemEntity>()
            .associateBy({ it.itemId }, { it.name })

        return purchases.map { p ->
            PurchaseWithItem(
                purchaseId = p.purchaseId,
                itemId = p.itemId,
                itemName = itemNameMap[p.itemId] ?: "Unknown item",
                itemPrice = p.itemPrice,
                shippingFee = p.shippingFee,
                protectionFee = p.protectionFee,
                totalAmount = p.totalAmount,
                createdAt = p.createdAt,
            )
        }
    }

    companion object {
        private const val SHIPPING_FEE = 3.95
        private const val PROTECTION_FEE = 0.90
    }
}
