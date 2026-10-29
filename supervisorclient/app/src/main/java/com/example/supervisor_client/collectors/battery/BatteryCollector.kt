package com.example.supervisor_client.collectors.battery

import android.content.Context
import com.example.supervisor_client.collectors.DataCollector
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.BatteryPingDao
import com.example.supervisor_client.data.local.BatteryPingEntity
import kotlinx.coroutines.CancellationException

class BatteryCollector internal constructor(
    private val dataSource: BatteryDataSource,
    private val dao: BatteryPingDao,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : DataCollector {

    // Keeps existing call sites like BatteryCollector(this, batteryDao) working
    constructor(context: Context, dao: BatteryPingDao) :
            this(AndroidBatteryDataSource(context), dao)

    suspend fun collectAndStorePing(): BatteryPingEntity {
        val now = nowMillis()
        val entity = try {
            val reading = dataSource.getCurrentReading()
            if (reading == null) {
                BatterySignalParser.createUnreachableEntity(
                    BatterySignalParser.STATUS_UNAVAILABLE, now
                )
            } else {
                BatterySignalParser.createSuccessEntity(reading, now)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            BatterySignalParser.createUnreachableEntity(BatterySignalParser.STATUS_ERROR, now)
        }

        try {
            dao.insertPing(entity)
        } finally {
            dao.purgePingsOlderThan(AppDatabase.thirtyDayCutoffMillis(now))
        }
        return entity
    }
}