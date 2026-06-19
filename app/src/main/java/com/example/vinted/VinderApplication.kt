package com.example.vinted

import android.app.Application
import com.example.vinted.data.AccountPreferences
import com.example.vinted.data.NotificationPreferences

class VinderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AccountPreferences.init(this)
        NotificationPreferences.init(this)
    }
}
