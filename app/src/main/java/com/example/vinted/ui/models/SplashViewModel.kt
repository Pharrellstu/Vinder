package com.example.vinted.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    var isAppReady by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MS)
            isAppReady = true
        }
    }

    companion object {
        private const val SPLASH_DURATION_MS = 3000L
    }
}
