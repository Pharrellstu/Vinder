package com.example.vinted.data

import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.AccountFavoriteEntity
import com.example.vinted.data.dto.ItemCategoryEntity
import com.example.vinted.data.dto.ItemConditionEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.data.dto.ItemOfferEntity
import com.example.vinted.data.dto.ItemPhotoEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.EditableItem
import com.example.vinted.ui.models.MyListing
import com.example.vinted.ui.models.OfferWithDetails
import com.example.vinted.ui.models.Product
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlin.math.roundToInt
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface IItemRepository {
    suspend fun getFeedItems(searchQuery: String? = null, offset: Int = 0): List<Product>
    suspend fun getCategories(): List<String>
    suspend fun getCategoryNames(): List<String>
    suspend fun getConditionNames(): List<String>
    suspend fun getCategoryId(categoryName: String): Int
    suspend fun getConditionId(conditionName: String): Int
    suspend fun getItemPhotos(itemId: Int): List<String>
    suspend fun getItemDescription(itemId: Int): String
    suspend fun markAsSold(itemId: Int)
    suspend fun createOffer(itemId: Int, creatorId: Int, offerPrice: Double)
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
    suspend fun uploadPhotoUnique(itemId: Int, bytes: ByteArray): String
    suspend fun insertItemPhoto(itemId: Int, photoUrl: String)
    suspend fun getMyListings(sellerId: Int): List<MyListing>
    suspend fun deleteItem(itemId: Int)
    suspend fun updateItem(
        itemId: Int,
        name: String,
        description: String,
        price: Double,
        categoryId: Int,
        conditionId: Int,
    )
    suspend fun getItemForEdit(itemId: Int): EditableItem
    suspend fun getItemPhotoRows(itemId: Int): List<ItemPhotoEntity>
    suspend fun deleteItemPhoto(photoId: Int, photoUrl: String)
    suspend fun getOffersForSeller(sellerId: Int): List<OfferWithDetails>
    suspend fun updateOfferStatus(offerId: Int, statusId: Int)
    suspend fun getFavoriteItemIds(accountId: Int): Set<Int>
    suspend fun getFavoriteItems(accountId: Int): List<Product>
    suspend fun addFavorite(accountId: Int, itemId: Int)
    suspend fun removeFavorite(accountId: Int, itemId: Int)
}

const val OFFER_STATUS_PENDING = 1
const val OFFER_STATUS_ACCEPTED = 2
const val OFFER_STATUS_REJECTED = 3

@Serializable
private data class OfferInsert(
    @SerialName("item_id") val itemId: Int,
    @SerialName("offer_creator_id") val offerCreatorId: Int,
    @SerialName("offer_price") val offerPrice: Double,
    @SerialName("offer_status_id") val offerStatusId: Int,
)

class ItemRepository(
    private val client: SupabaseClient = SupabaseClientInitialiser.client,
) : IItemRepository {

    companion object {
        const val PAGE_SIZE = 20
    }

    override suspend fun getCategories(): List<String> {
        // Filter UI prepends the "All" pass-through; selectable lists (e.g. Add
        // Product) use getCategoryNames() to get the real names only.
        return listOf("All") + getCategoryNames()
    }

    override suspend fun getCategoryNames(): List<String> {
        return client.from("item_category")
            .select()
            .decodeList<ItemCategoryEntity>()
            .map { it.name }
            .distinct()
    }

    override suspend fun getConditionNames(): List<String> {
        return client.from("item_condition")
            .select()
            .decodeList<ItemConditionEntity>()
            .map { it.name }
            .distinct()
    }

    override suspend fun getCategoryId(categoryName: String): Int {
        // decodeList().firstOrNull() rather than decodeSingle(): the lookup tables
        // can contain duplicate names (from repeated seeding) and the name may be
        // blank, both of which make decodeSingle() throw ("List is empty." / 406).
        return client.from("item_category")
            .select { filter { eq("category_name", categoryName) } }
            .decodeList<ItemCategoryEntity>()
            .firstOrNull()?.categoryId
            ?: error("Category \"$categoryName\" is not available.")
    }

    override suspend fun getConditionId(conditionName: String): Int {
        return client.from("item_condition")
            .select { filter { eq("item_condition_name", conditionName) } }
            .decodeList<ItemConditionEntity>()
            .firstOrNull()?.conditionId
            ?: error("Condition \"$conditionName\" is not available.")
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
            buildJsonObject {
                put("seller_id", sellerId)
                put("item_category_id", categoryId)
                put("item_condition_id", conditionId)
                put("item_name", name)
                put("item_description", description)
                put("item_price", price)
                put("is_listed", false)
                put("is_sold", false)
            }
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

    // Edit-mode uploads use a unique name so they never clobber an item's existing
    // photo_$index.jpg files (which the create flow names by position).
    override suspend fun uploadPhotoUnique(itemId: Int, bytes: ByteArray): String {
        val path = "items/$itemId/photo_${System.currentTimeMillis()}.jpg"
        client.storage["item-photos"].upload(path, bytes)
        return client.storage["item-photos"].publicUrl(path)
    }

    override suspend fun insertItemPhoto(itemId: Int, photoUrl: String) {
        client.from("item_photo").insert(
            buildJsonObject {
                put("item_id", itemId)
                put("photo_url", photoUrl)
            }
        )
    }

    override suspend fun getItemPhotos(itemId: Int): List<String> {
        // Sort by item_photo_id so the gallery order matches upload order (the create
        // flow uploads photo_0 first), keeping the cover photo at index 0. Postgrest
        // does not guarantee row order without an explicit sort.
        return client.from("item_photo")
            .select { filter { eq("item_id", itemId) } }
            .decodeList<ItemPhotoEntity>()
            .sortedBy { it.itemPhotoId }
            .map { it.photoUrl }
    }

    override suspend fun getItemDescription(itemId: Int): String {
        return client.from("item")
            .select { filter { eq("item_id", itemId) } }
            .decodeSingle<ItemEntity>()
            .description
    }

    override suspend fun markAsSold(itemId: Int) {
        client.from("item").update(mapOf("is_sold" to true)) {
            filter { eq("item_id", itemId) }
        }
    }

    override suspend fun createOffer(itemId: Int, creatorId: Int, offerPrice: Double) {
        client.from("item_offer").insert(
            OfferInsert(itemId, creatorId, offerPrice, offerStatusId = OFFER_STATUS_PENDING)
        )
    }

    override suspend fun getOffersForSeller(sellerId: Int): List<OfferWithDetails> {
        val sellerItems = client.from("item")
            .select { filter { eq("seller_id", sellerId) } }
            .decodeList<ItemEntity>()

        if (sellerItems.isEmpty()) return emptyList()

        val itemIds = sellerItems.map { it.itemId }
        val itemNameMap = sellerItems.associateBy({ it.itemId }, { it.name })

        val offers = client.from("item_offer")
            .select { filter { isIn("item_id", itemIds) } }
            .decodeList<ItemOfferEntity>()

        if (offers.isEmpty()) return emptyList()

        val buyerIds = offers.map { it.offerCreatorId }.distinct()
        val buyerMap = client.from("account")
            .select { filter { isIn("account_id", buyerIds) } }
            .decodeList<AccountEntity>()
            .associateBy { it.accountId }

        return offers.map { offer ->
            OfferWithDetails(
                offerId = offer.itemOfferId,
                itemId = offer.itemId,
                itemName = itemNameMap[offer.itemId] ?: "Unknown item",
                buyerId = offer.offerCreatorId,
                buyerName = buyerMap[offer.offerCreatorId]?.accountName ?: "Unknown buyer",
                offerPrice = offer.offerPrice,
                statusId = offer.offerStatusId,
                createdAt = offer.createdAt,
            )
        }
    }

    override suspend fun updateOfferStatus(offerId: Int, statusId: Int) {
        client.from("item_offer").update(mapOf("offer_status_id" to statusId)) {
            filter { eq("item_offer_id", offerId) }
        }
    }

    override suspend fun getFeedItems(searchQuery: String?, offset: Int): List<Product> {
        val items = client.from("item")
            .select {
                filter {
                    eq("is_listed", true)
                    if (!searchQuery.isNullOrBlank()) {
                        ilike("item_name", "%$searchQuery%")
                    }
                }
                range(offset.toLong(), (offset + PAGE_SIZE - 1).toLong())
            }
            .decodeList<ItemEntity>()
        val favoriteItemIds = getFavoriteItemIds(SessionManager.currentAccountId)
        return buildProducts(items, favoriteItemIds)
    }

    override suspend fun getFavoriteItemIds(accountId: Int): Set<Int> {
        if (accountId == SessionManager.NO_ACCOUNT_ID) return emptySet()
        return client.from("account_favorite")
            .select { filter { eq("account_id", accountId) } }
            .decodeList<AccountFavoriteEntity>()
            .map { it.itemId }
            .toSet()
    }

    override suspend fun getFavoriteItems(accountId: Int): List<Product> {
        val favoriteItemIds = getFavoriteItemIds(accountId)
        if (favoriteItemIds.isEmpty()) return emptyList()
        val items = client.from("item")
            .select { filter { isIn("item_id", favoriteItemIds.toList()) } }
            .decodeList<ItemEntity>()
        return buildProducts(items, favoriteItemIds)
    }

    override suspend fun addFavorite(accountId: Int, itemId: Int) {
        client.from("account_favorite").insert(
            buildJsonObject {
                put("account_id", accountId)
                put("item_id", itemId)
            }
        )
    }

    override suspend fun removeFavorite(accountId: Int, itemId: Int) {
        client.from("account_favorite").delete {
            filter {
                eq("account_id", accountId)
                eq("item_id", itemId)
            }
        }
    }

    override suspend fun getMyListings(sellerId: Int): List<MyListing> {
        val items = client.from("item")
            .select { filter { eq("seller_id", sellerId) } }
            .decodeList<ItemEntity>()
        if (items.isEmpty()) return emptyList()

        val coverUrlByItem = client.coverUrlsByItem(items.map { it.itemId })
        // Newest first so a just-posted item appears at the top.
        return items.sortedByDescending { it.itemId }.map { item ->
            MyListing(
                itemId = item.itemId,
                name = item.name,
                price = item.price.roundToInt(),
                coverUrl = coverUrlByItem[item.itemId],
                isSold = item.isSold,
            )
        }
    }

    override suspend fun deleteItem(itemId: Int) {
        // Remove storage objects first (DB cascade drops the item_photo rows for us).
        val photos = runCatching { getItemPhotoRows(itemId) }.getOrDefault(emptyList())
        photos.forEach { removeStorageObject(it.photoUrl) }
        client.from("item").delete { filter { eq("item_id", itemId) } }
    }

    override suspend fun updateItem(
        itemId: Int,
        name: String,
        description: String,
        price: Double,
        categoryId: Int,
        conditionId: Int,
    ) {
        client.from("item").update(
            buildJsonObject {
                put("item_name", name)
                put("item_description", description)
                put("item_price", price)
                put("item_category_id", categoryId)
                put("item_condition_id", conditionId)
            }
        ) {
            filter { eq("item_id", itemId) }
        }
    }

    override suspend fun getItemForEdit(itemId: Int): EditableItem {
        val item = client.from("item")
            .select { filter { eq("item_id", itemId) } }
            .decodeSingle<ItemEntity>()

        val categoryName = client.from("item_category")
            .select { filter { eq("item_category_id", item.categoryId) } }
            .decodeList<ItemCategoryEntity>()
            .firstOrNull()?.name ?: ""

        val conditionName = client.from("item_condition")
            .select { filter { eq("item_condition_id", item.conditionId) } }
            .decodeList<ItemConditionEntity>()
            .firstOrNull()?.name ?: ""

        return EditableItem(
            itemId = item.itemId,
            name = item.name,
            description = item.description,
            price = item.price,
            category = categoryName,
            condition = conditionName,
            photos = getItemPhotoRows(itemId),
        )
    }

    override suspend fun getItemPhotoRows(itemId: Int): List<ItemPhotoEntity> {
        return client.from("item_photo")
            .select { filter { eq("item_id", itemId) } }
            .decodeList<ItemPhotoEntity>()
            .sortedBy { it.itemPhotoId }
    }

    override suspend fun deleteItemPhoto(photoId: Int, photoUrl: String) {
        client.from("item_photo").delete { filter { eq("item_photo_id", photoId) } }
        removeStorageObject(photoUrl)
    }

    /** Best-effort removal of a stored object given its public URL; never throws. */
    private suspend fun removeStorageObject(publicUrl: String) {
        val path = publicUrl.substringAfter("/item-photos/", "")
        if (path.isBlank()) return
        runCatching { client.storage["item-photos"].delete(listOf(path)) }
    }

    private suspend fun buildProducts(items: List<ItemEntity>, favoriteItemIds: Set<Int>): List<Product> {
        val sellerIds = items.map { it.sellerId }.distinct()
        val sellerMap: Map<Int, AccountEntity> = if (sellerIds.isNotEmpty()) {
            client.from("account")
                .select { filter { isIn("account_id", sellerIds) } }
                .decodeList<AccountEntity>()
                .associateBy { it.accountId }
        } else emptyMap()

        val categoryNameById: Map<Int, String> = client.from("item_category")
            .select()
            .decodeList<ItemCategoryEntity>()
            .associateBy({ it.categoryId }, { it.name })

        val conditionNameById: Map<Int, String> = client.from("item_condition")
            .select()
            .decodeList<ItemConditionEntity>()
            .associateBy({ it.conditionId }, { it.name })

        val coverUrlByItem = client.coverUrlsByItem(items.map { it.itemId })

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
                category = categoryNameById[item.categoryId] ?: "",
                condition = conditionNameById[item.conditionId] ?: "",
                isFavorite = item.itemId in favoriteItemIds,
                coverImageUrl = coverUrlByItem[item.itemId],
            )
        }
    }
}
