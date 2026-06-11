package com.example.vinted.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountEntity(
    @SerialName("account_id") val accountId: Int,
    @SerialName("account_name") val accountName: String,
    @SerialName("account_email") val accountEmail: String,
)
