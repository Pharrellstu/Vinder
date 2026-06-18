package com.example.vinted.data

import androidx.compose.ui.graphics.Color
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.AccountSideInfoEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.data.dto.ItemPhotoEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.ListingItem
import com.example.vinted.ui.models.UserProfile
import com.example.vinted.ui.theme.VinderAzure
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FollowRow(@SerialName("following_id") val followingId: Int)

interface IAccountRepository {
    suspend fun getProfile(accountId: Int): UserProfile
    suspend fun getListedItems(accountId: Int): List<ListingItem>
    suspend fun getSoldItems(accountId: Int): List<ListingItem>
    suspend fun getFollowerCount(accountId: Int): Int
    suspend fun updateProfile(accountId: Int, name: String, bio: String, location: String)
    suspend fun updateAvatar(accountId: Int, bytes: ByteArray): String
}

class AccountRepository : IAccountRepository {

    private val client = SupabaseClientInitialiser.client
    private val ITEM_DEFAULT_BG_COLOR = Color(0xFFE5E5EA)

    override suspend fun getProfile(accountId: Int): UserProfile {
        val account = client.from("account")
            .select { filter { eq("account_id", accountId) } }
            .decodeSingle<AccountEntity>()

        val sideInfo = client.from("account_side_information")
            .select { filter { eq("account_id", accountId) } }
            .decodeSingleOrNull<AccountSideInfoEntity>()

        val listedCount = client.from("item")
            .select {
                filter {
                    eq("seller_id", accountId)
                    eq("is_listed", true)
                }
            }.decodeList<ItemEntity>().size

        val soldCount = client.from("item")
            .select {
                filter {
                    eq("seller_id", accountId)
                    eq("is_sold", true)
                }
            }.decodeList<ItemEntity>().size

        val followerCount = getFollowerCount(accountId)

        return UserProfile(
            handle = account.accountName,
            initial = account.accountName.first().uppercase(),
            avatarColor = VinderAzure,
            rating = 0f,
            reviewCount = 0,
            location = sideInfo?.location ?: "",
            bio = sideInfo?.bio ?: "",
            listedCount = listedCount,
            soldCount = soldCount,
            followerCount = followerCount,
            avatarUrl = sideInfo?.profilePictureUrl,
        )
    }

    override suspend fun getListedItems(accountId: Int): List<ListingItem> {
        val items = client.from("item")
            .select {
                filter {
                    eq("seller_id", accountId)
                    eq("is_listed", true)
                }
            }
            .decodeList<ItemEntity>()
        return items.toListingItems()
    }

    override suspend fun getSoldItems(accountId: Int): List<ListingItem> {
        val items = client.from("item")
            .select {
                filter {
                    eq("seller_id", accountId)
                    eq("is_sold", true)
                }
            }
            .decodeList<ItemEntity>()
        return items.toListingItems()
    }

    private suspend fun List<ItemEntity>.toListingItems(): List<ListingItem> {
        val covers = coverUrlsByItem(map { it.itemId })
        return map { item ->
            ListingItem(
                id = item.itemId.toString(),
                price = item.price.roundToInt(),
                bgColor = ITEM_DEFAULT_BG_COLOR,
                coverUrl = covers[item.itemId],
            )
        }
    }

    /** Cover photo per item = the first uploaded photo (lowest item_photo_id). */
    private suspend fun coverUrlsByItem(itemIds: List<Int>): Map<Int, String> {
        if (itemIds.isEmpty()) return emptyMap()
        return client.from("item_photo")
            .select { filter { isIn("item_id", itemIds) } }
            .decodeList<ItemPhotoEntity>()
            .groupBy { it.itemId }
            .mapValues { (_, photos) -> photos.minByOrNull { it.itemPhotoId }!!.photoUrl }
    }

    override suspend fun getFollowerCount(accountId: Int): Int {
        return client.from("account_following")
            .select { filter { eq("following_id", accountId) } }
            .decodeList<FollowRow>().size
    }

    override suspend fun updateProfile(accountId: Int, name: String, bio: String, location: String) {
        client.from("account").update(mapOf("account_name" to name)) {
            filter { eq("account_id", accountId) }
        }

        // account_side_information holds one row per account (UNIQUE account_id);
        // update it in place when present, otherwise create it.
        val existing = client.from("account_side_information")
            .select { filter { eq("account_id", accountId) } }
            .decodeSingleOrNull<AccountSideInfoEntity>()

        if (existing == null) {
            client.from("account_side_information").insert(
                mapOf(
                    "account_id" to accountId,
                    "account_bio" to bio,
                    "account_location" to location,
                ),
            )
        } else {
            client.from("account_side_information").update(
                mapOf(
                    "account_bio" to bio,
                    "account_location" to location,
                ),
            ) {
                filter { eq("account_id", accountId) }
            }
        }
    }

    override suspend fun updateAvatar(accountId: Int, bytes: ByteArray): String {
        val path = "avatars/$accountId/${System.currentTimeMillis()}.jpg"
        client.storage["item-photos"].upload(path, bytes)
        val url = client.storage["item-photos"].publicUrl(path)

        val existing = client.from("account_side_information")
            .select { filter { eq("account_id", accountId) } }
            .decodeSingleOrNull<AccountSideInfoEntity>()

        if (existing == null) {
            client.from("account_side_information").insert(
                mapOf("account_id" to accountId, "account_profile_picture_url" to url),
            )
        } else {
            client.from("account_side_information").update(
                mapOf("account_profile_picture_url" to url),
            ) {
                filter { eq("account_id", accountId) }
            }
        }
        return url
    }
}
