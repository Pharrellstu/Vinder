package com.example.vinted.data

import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.ItemCategoryEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.Product
import io.github.jan.supabase.postgrest.from

interface IItemRepository {
    suspend fun getFeedItems(): List<Product>
    suspend fun getCategories(): List<String>
}

class ItemRepository : IItemRepository {

    private val client = SupabaseClientInitialiser.client

    override suspend fun getCategories(): List<String> {
        val cats = client.from("item_category")
            .select()
            .decodeList<ItemCategoryEntity>()
        return listOf("All") + cats.map { it.name }
    }

    override suspend fun getFeedItems(): List<Product> {
        val items = client.from("item")
            .select { filter { eq("is_listed", true) } }
            .decodeList<ItemEntity>()

        return items.map { item ->
            val seller = runCatching {
                client.from("account")
                    .select { filter { eq("account_id", item.sellerId) } }
                    .decodeSingle<AccountEntity>()
            }.getOrNull()

            Product(
                id = item.itemId.toString(),
                name = item.name,
                price = item.price.toFloat(),
                originalPrice = if (item.discount != null && item.discount > 0) {
                    val original = item.price / (1.0 - item.discount / 100.0)
                    original.toFloat()
                } else null,
                discountPercent = item.discount,
                size = null,
                brand = null,
                sellerInitial = seller?.accountName?.first()?.uppercase() ?: "?",
                sellerName = seller?.accountName ?: "unknown",
                rating = 0f,
            )
        }
    }
}
