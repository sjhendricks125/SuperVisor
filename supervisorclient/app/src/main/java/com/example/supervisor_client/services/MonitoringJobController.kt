package com.example.supervisor_client.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class MonitoringJobController(
    private val scope: CoroutineScope
) {
    private var monitoringJob: Job? = null

    fun start(block: suspend () -> Unit) {
        val previousJob = monitoringJob
        previousJob?.cancel()
        monitoringJob = scope.launch {
            previousJob?.join()
            block()
        }
    }

    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
}
