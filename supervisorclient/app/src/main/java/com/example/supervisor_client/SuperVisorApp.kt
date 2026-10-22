package com.example.supervisor_client

import android.app.Application
import com.example.supervisor_client.data.local.AppDatabase

class SuperVisorApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
