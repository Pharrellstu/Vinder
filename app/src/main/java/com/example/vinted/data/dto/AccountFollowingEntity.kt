package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountFollowingEntity(
    @SerialName("account_following_id") val accountFollowingId: Int,
    @SerialName("follower_id") val followerId: Int,
    @SerialName("following_id") val followingId: Int,
)
