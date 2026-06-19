package com.example.vinted.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinted.data.AccountRepository
import com.example.vinted.data.IAccountRepository
import com.example.vinted.data.SessionManager
import com.example.vinted.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: UserProfile,
        val listings: List<ListingItem>,
        val soldItems: List<ListingItem>,
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val accountId: Int? = null,
    private val repository: IAccountRepository = AccountRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    // True when viewing someone else's profile (not the signed-in user's own).
    private val isSellerProfile: Boolean
        get() = accountId != null && accountId != SessionManager.currentAccountId

    init {
        load()
    }

    fun load() {
        val id = accountId ?: SessionManager.currentAccountId
        if (id == -1) {
            _uiState.value = ProfileUiState.Error("Not logged in")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            runCatching {
                val profile = repository.getProfile(id)
                val listings = repository.getListedItems(id)
                val soldItems = repository.getSoldItems(id)
                if (isSellerProfile) {
                    _isFollowing.value = repository.isFollowing(id)
                }
                ProfileUiState.Success(profile, listings, soldItems)
            }.onSuccess { _uiState.value = it }
             .onFailure { _uiState.value = ProfileUiState.Error(ErrorMessages.friendlyMessage(it, "Failed to load profile")) }
        }
    }

    fun onFollow() {
        val id = accountId ?: return
        val prev = _isFollowing.value
        _isFollowing.value = true
        updateFollowerCount(+1)
        viewModelScope.launch {
            runCatching { repository.follow(id) }
                .onFailure {
                    _isFollowing.value = prev
                    updateFollowerCount(-1)
                }
        }
    }

    fun onUnfollow() {
        val id = accountId ?: return
        val prev = _isFollowing.value
        _isFollowing.value = false
        updateFollowerCount(-1)
        viewModelScope.launch {
            runCatching { repository.unfollow(id) }
                .onFailure {
                    _isFollowing.value = prev
                    updateFollowerCount(+1)
                }
        }
    }

    private fun updateFollowerCount(delta: Int) {
        val current = _uiState.value as? ProfileUiState.Success ?: return
        val updated = current.profile.copy(followerCount = (current.profile.followerCount + delta).coerceAtLeast(0))
        _uiState.value = current.copy(profile = updated)
    }
}
