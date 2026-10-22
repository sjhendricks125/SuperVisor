package com.example.supervisor_client.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // TODO: Implement future sync and 30-day purge.
        return Result.success()
    }
}
