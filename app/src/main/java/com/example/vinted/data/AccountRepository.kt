package com.example.vinted.data

import androidx.compose.ui.graphics.Color
import com.example.vinted.data.dto.AccountEntity
import com.example.vinted.data.dto.AccountSideInfoEntity
import com.example.vinted.data.dto.ItemEntity
import com.example.vinted.ui.initialisers.SupabaseClientInitialiser
import com.example.vinted.ui.models.ListingItem
import com.example.vinted.ui.models.UserProfile
import com.example.vinted.ui.theme.VinderAzure
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import kotlin.math.roundToInt

interface IAccountRepository {
    suspend fun getProfile(accountId: Int): UserProfile
    suspend fun getListedItems(accountId: Int): List<ListingItem>
    suspend fun getSoldItems(accountId: Int): List<ListingItem>
    suspend fun getFollowerCount(accountId: Int): Int
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
            .select(count = Count.EXACT) {
                filter {
                    eq("seller_id", accountId)
                    eq("is_listed", true)
                }
            }.countOrThrow().toInt()

        val soldCount = client.from("item")
            .select(count = Count.EXACT) {
                filter {
                    eq("seller_id", accountId)
                    eq("is_sold", true)
                }
            }.countOrThrow().toInt()

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
        )
    }

    override suspend fun getListedItems(accountId: Int): List<ListingItem> {
        return client.from("item")
            .select {
                filter {
                    eq("seller_id", accountId)
                    eq("is_listed", true)
                }
            }
            .decodeList<ItemEntity>()
            .map { item ->
                ListingItem(
                    id = item.itemId.toString(),
                    price = item.price.roundToInt(),
                    bgColor = ITEM_DEFAULT_BG_COLOR,
                )
            }
    }

    override suspend fun getSoldItems(accountId: Int): List<ListingItem> {
        return client.from("item")
            .select {
                filter {
                    eq("seller_id", accountId)
                    eq("is_sold", true)
                }
            }
            .decodeList<ItemEntity>()
            .map { item ->
                ListingItem(
                    id = item.itemId.toString(),
                    price = item.price.roundToInt(),
                    bgColor = ITEM_DEFAULT_BG_COLOR,
                )
            }
    }

    override suspend fun getFollowerCount(accountId: Int): Int {
        return client.from("account_following")
            .select(count = Count.EXACT) {
                filter { eq("following_id", accountId) }
            }.countOrThrow().toInt()
    }
}
