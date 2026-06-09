package com.example.vinted.ui.models

import androidx.compose.ui.graphics.Color

data class UserProfile(
    val handle: String,
    val initial: String,
    val avatarColor: Color,
    val rating: Float,
    val reviewCount: Int,
    val location: String,
    val bio: String,
    val listedCount: Int,
    val soldCount: Int,
    val followerCount: Int,
)
