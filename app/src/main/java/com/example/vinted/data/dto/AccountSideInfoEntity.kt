package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountSideInfoEntity(
    @SerialName("account_side_information_id") val id: Int,
    @SerialName("account_id") val accountId: Int,
    @SerialName("account_bio") val bio: String? = null,
    @SerialName("account_location") val location: String? = null,
    @SerialName("account_profile_picture_url") val profilePictureUrl: String? = null,
    @SerialName("account_public_url") val publicUrl: String? = null,
)
