package com.example.supervisor_client.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.supervisor_client.SuperVisorApp
import com.example.supervisor_client.data.local.AppDatabase

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = (applicationContext as SuperVisorApp).database
        val cutoff = AppDatabase.thirtyDayCutoffMillis()
        val dao = database.locationPingDao()
        dao.purgePingsOlderThan(cutoff)
        database.messageNotificationLogDao().purgeLogsOlderThan(cutoff)
        val unsynced = dao.getUnsyncedPings()
        // TODO: Sync unsynced pings with ApiSyncClient and pass remote officer interval to MonitoringConfigStore.setPingIntervalMinutes()
        return Result.success()
    }
}
