package com.example.vinted.ui.models

import com.example.vinted.data.dto.ItemPhotoEntity

/** Existing listing data used to prefill the Add Product form in edit mode. */
data class EditableItem(
    val itemId: Int,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String,
    val photos: List<ItemPhotoEntity>,
)
