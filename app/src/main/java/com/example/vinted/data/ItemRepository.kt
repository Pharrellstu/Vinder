package com.example.vinted.data

import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.ItemCategoryEntity
import com.example.vinted.data.dto.ItemConditionEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.Product
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage

interface IItemRepository {
    suspend fun getFeedItems(): List<Product>
    suspend fun getCategories(): List<String>
    suspend fun getCategoryId(categoryName: String): Int
    suspend fun getConditionId(conditionName: String): Int
    suspend fun insertItem(
        sellerId: Int,
        categoryId: Int,
        conditionId: Int,
        name: String,
        description: String,
        price: Double,
    ): Int
    suspend fun updateItemToListed(itemId: Int)
    suspend fun uploadPhoto(itemId: Int, index: Int, bytes: ByteArray): String
    suspend fun insertItemPhoto(itemId: Int, photoUrl: String)
}

class ItemRepository : IItemRepository {

    private val client = SupabaseClientInitialiser.client

    override suspend fun getCategories(): List<String> {
        val cats = client.from("item_category")
            .select()
            .decodeList<ItemCategoryEntity>()
        return listOf("All") + cats.map { it.name }
    }

    override suspend fun getCategoryId(categoryName: String): Int {
        return client.from("item_category")
            .select { filter { eq("category_name", categoryName) } }
            .decodeSingle<ItemCategoryEntity>().categoryId
    }

    override suspend fun getConditionId(conditionName: String): Int {
        return client.from("item_condition")
            .select { filter { eq("item_condition_name", conditionName) } }
            .decodeSingle<ItemConditionEntity>().conditionId
    }

    override suspend fun insertItem(
        sellerId: Int,
        categoryId: Int,
        conditionId: Int,
        name: String,
        description: String,
        price: Double,
    ): Int {
        val result = client.from("item").insert(
            mapOf(
                "seller_id" to sellerId,
                "item_category_id" to categoryId,
                "item_condition_id" to conditionId,
                "item_name" to name,
                "item_description" to description,
                "item_price" to price,
                "is_listed" to false,
                "is_sold" to false,
                "created_at" to java.time.Instant.now().toString(),
            )
        ) { select() }
        return result.decodeSingle<ItemEntity>().itemId
    }

    override suspend fun updateItemToListed(itemId: Int) {
        client.from("item").update(mapOf("is_listed" to true)) {
            filter { eq("item_id", itemId) }
        }
    }

    override suspend fun uploadPhoto(itemId: Int, index: Int, bytes: ByteArray): String {
        val path = "items/$itemId/photo_$index.jpg"
        client.storage["item-photos"].upload(path, bytes)
        return client.storage["item-photos"].publicUrl(path)
    }

    override suspend fun insertItemPhoto(itemId: Int, photoUrl: String) {
        client.from("item_photo").insert(
            mapOf("item_id" to itemId, "photo_url" to photoUrl)
        )
    }

    override suspend fun getFeedItems(): List<Product> {
        val items = client.from("item")
            .select { filter { eq("is_listed", true) } }
            .decodeList<ItemEntity>()

        val sellerIds = items.map { it.sellerId }.distinct()
        val sellerMap: Map<Int, AccountEntity> = if (sellerIds.isNotEmpty()) {
            client.from("account")
                .select { filter { isIn("account_id", sellerIds) } }
                .decodeList<AccountEntity>()
                .associateBy { it.accountId }
        } else emptyMap()

        return items.map { item ->
            val seller = sellerMap[item.sellerId]

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
                sellerInitial = seller?.accountName?.firstOrNull()?.uppercase() ?: "?",
                sellerName = seller?.accountName ?: "unknown",
                rating = 0f,
                sellerId = item.sellerId,
            )
        }
    }
}
