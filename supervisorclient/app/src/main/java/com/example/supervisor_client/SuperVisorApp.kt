package com.example.supervisor_client

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.MessageLogDatabase
import com.example.supervisor_client.workers.SyncWorker
import java.util.concurrent.TimeUnit

class SuperVisorApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val messageLogDatabase: MessageLogDatabase by lazy {
        MessageLogDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        val retentionWork = PeriodicWorkRequestBuilder<SyncWorker>(
            1,
            TimeUnit.DAYS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            retentionWork
        )
    }

    private companion object {
        const val PERIODIC_SYNC_WORK_NAME = "location_ping_maintenance"
    }
}
