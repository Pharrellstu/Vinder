package com.example.vinted.data

import com.example.vinted.data.dto.ItemPhotoEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/** Cover photo per item = the first uploaded photo (lowest item_photo_id). */
internal suspend fun SupabaseClient.coverUrlsByItem(itemIds: List<Int>): Map<Int, String> {
    if (itemIds.isEmpty()) return emptyMap()
    return from("item_photo")
        .select { filter { isIn("item_id", itemIds) } }
        .decodeList<ItemPhotoEntity>()
        .groupBy { it.itemId }
        .mapValues { (_, photos) -> photos.minByOrNull { it.itemPhotoId }!!.photoUrl }
}
