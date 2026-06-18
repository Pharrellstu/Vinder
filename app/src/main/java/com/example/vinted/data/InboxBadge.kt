package com.example.vinted.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * App-wide unread message count so the bottom-nav inbox badge is visible from
 * any tab, not just while the Messages screen is open.
 */
object InboxBadge {
    val unread = MutableStateFlow(0)

    fun clear() {
        unread.value = 0
    }
}
