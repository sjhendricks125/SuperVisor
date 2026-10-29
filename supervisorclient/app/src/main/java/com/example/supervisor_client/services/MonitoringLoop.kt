package com.example.supervisor_client.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal suspend fun runMonitoringLoop(
    collectPing: suspend () -> Unit,
    intervalMinutes: () -> Long,
    onError: (Exception) -> Unit,
    waitForNextPing: suspend (Long) -> Unit = { delay(it) }
) {
    while (currentCoroutineContext().isActive) {
        try {
            collectPing()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            onError(exception)
        }

        waitForNextPing(pingIntervalMillis(intervalMinutes()))
    }
}

internal fun pingIntervalMillis(intervalMinutes: Long): Long =
    intervalMinutes
        .coerceAtLeast(1L)
        .coerceAtMost(Long.MAX_VALUE / MILLIS_PER_MINUTE) * MILLIS_PER_MINUTE

private const val MILLIS_PER_MINUTE = 60_000L
