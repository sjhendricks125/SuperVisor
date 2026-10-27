package com.example.supervisor_client.collectors.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.example.supervisor_client.collectors.DataCollector
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.BatteryPingDao
import com.example.supervisor_client.data.local.BatteryPingEntity
import kotlinx.coroutines.CancellationException

class BatteryCollector(
    private val context: Context,
    private val dao: BatteryPingDao
) : DataCollector {

    suspend fun collectAndStorePing(): BatteryPingEntity {
        val entity = try {
            // null receiver = just return the current sticky broadcast
            val intent = context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            if (intent == null) {
                BatterySignalParser.createUnreachableEntity(
                    BatterySignalParser.STATUS_UNAVAILABLE
                )
            } else {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                BatterySignalParser.createSuccessEntity(intent, pm.isPowerSaveMode)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            BatterySignalParser.createUnreachableEntity(BatterySignalParser.STATUS_ERROR)
        }

        try {
            dao.insertPing(entity)
        } finally {
            dao.purgePingsOlderThan(AppDatabase.thirtyDayCutoffMillis())
        }
        return entity
    }
}