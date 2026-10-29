package com.example.supervisor_client.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class InMemoryBatteryPingDao : BatteryPingDao {
    private var nextId = 1L
    private val currentPings = mutableListOf<BatteryPingEntity>()
    private val pingsState = MutableStateFlow<List<BatteryPingEntity>>(emptyList())

    val pings: List<BatteryPingEntity>
        get() = currentPings.toList()

    var lastPurgeCutoff: Long? = null
        private set

    override suspend fun insertPing(ping: BatteryPingEntity): Long {
        val id = nextId++
        currentPings += ping.copy(id = id)
        publish()
        return id
    }

    override suspend fun getUnsyncedPings(): List<BatteryPingEntity> =
        currentPings.filterNot { it.isSynced }.sortedBy { it.timestamp }

    override suspend fun markPingsAsSynced(ids: List<Long>) {
        val syncedIds = ids.toSet()
        for (index in currentPings.indices) {
            val ping = currentPings[index]
            if (ping.id in syncedIds) {
                currentPings[index] = ping.copy(isSynced = true)
            }
        }
        publish()
    }

    override fun getAllPingsFlow(): Flow<List<BatteryPingEntity>> = pingsState

    override suspend fun purgePingsOlderThan(cutoffTimestamp: Long): Int {
        lastPurgeCutoff = cutoffTimestamp
        val originalSize = currentPings.size
        currentPings.removeAll { it.timestamp < cutoffTimestamp }
        publish()
        return originalSize - currentPings.size
    }

    private fun publish() {
        pingsState.value = currentPings.sortedByDescending { it.timestamp }
    }
}