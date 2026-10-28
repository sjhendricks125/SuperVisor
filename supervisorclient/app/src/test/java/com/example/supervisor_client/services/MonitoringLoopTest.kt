package com.example.supervisor_client.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringLoopTest {
    @Test
    fun collectsImmediatelyThenWaitsAndRepeatsWithoutOverlap() = runTest {
        val events = mutableListOf<String>()
        val waits = mutableListOf<Long>()
        var collectionCount = 0
        var activeCollections = 0
        var maximumActiveCollections = 0
        val collectionReleases = Channel<Unit>(Channel.UNLIMITED)
        val waitReleases = Channel<Unit>(Channel.UNLIMITED)

        val job = launch {
            runMonitoringLoop(
                collectPing = {
                    activeCollections++
                    maximumActiveCollections =
                        maxOf(maximumActiveCollections, activeCollections)
                    collectionCount++
                    events += "collect-start:$collectionCount"
                    collectionReleases.receive()
                    events += "collect-end:$collectionCount"
                    activeCollections--
                },
                intervalMinutes = { collectionCount.toLong() },
                onError = { throw AssertionError("Unexpected collection failure", it) },
                waitForNextPing = { delayMillis ->
                    waits += delayMillis
                    events += "wait:$delayMillis"
                    waitReleases.receive()
                }
            )
        }

        runCurrent()
        assertEquals(listOf("collect-start:1"), events)
        collectionReleases.send(Unit)
        runCurrent()
        assertEquals(
            listOf("collect-start:1", "collect-end:1", "wait:60000"),
            events
        )

        waitReleases.send(Unit)
        runCurrent()
        assertEquals(
            listOf(
                "collect-start:1",
                "collect-end:1",
                "wait:60000",
                "collect-start:2"
            ),
            events
        )
        collectionReleases.send(Unit)
        runCurrent()
        assertEquals(120_000L, waits.lastOrNull() ?: 0L)
        assertEquals(1, maximumActiveCollections)

        job.cancelAndJoin()
    }

    @Test
    fun readsTheConfiguredIntervalAgainAfterEachCollection() = runTest {
        val waits = mutableListOf<Long>()
        var collectionCount = 0
        val waitReleases = Channel<Unit>(Channel.UNLIMITED)

        val job = launch {
            runMonitoringLoop(
                collectPing = { collectionCount++ },
                intervalMinutes = { collectionCount.toLong() },
                onError = { throw AssertionError("Unexpected collection failure", it) },
                waitForNextPing = { delayMillis ->
                    waits += delayMillis
                    if (waits.size == 2) {
                        throw CancellationException("end test loop")
                    } else {
                        waitReleases.receive()
                    }
                }
            )
        }

        runCurrent()
        assertEquals(listOf(60_000L), waits)
        waitReleases.send(Unit)
        job.join()
        assertEquals(listOf(60_000L, 120_000L), waits)
        assertEquals(2, collectionCount)
        assertTrue(job.isCancelled)
    }

    @Test
    fun reportsCollectionErrorsAndContinuesOnTheNextInterval() = runTest {
        var collectionCount = 0
        var reportedError: Exception? = null

        val job = launch {
            runMonitoringLoop(
                collectPing = {
                    collectionCount++
                    if (collectionCount == 1) {
                        throw IllegalStateException("temporary failure")
                    }
                },
                intervalMinutes = { 1L },
                onError = { reportedError = it },
                waitForNextPing = { delayMillis ->
                    assertEquals(60_000L, delayMillis)
                    throw CancellationException("end test loop")
                }
            )
        }

        job.join()

        assertEquals(1, collectionCount)
        assertTrue(reportedError is IllegalStateException)
        assertTrue(job.isCancelled)
    }

    @Test
    fun intervalDelayIsClampedAndCannotOverflow() {
        assertEquals(60_000L, pingIntervalMillis(0L))
        assertEquals(60_000L, pingIntervalMillis(-1L))
        assertEquals(120_000L, pingIntervalMillis(2L))
        assertEquals(
            Long.MAX_VALUE / 60_000L * 60_000L,
            pingIntervalMillis(Long.MAX_VALUE)
        )
    }

    @Test
    fun replacingMonitoringJobWaitsForPreviousJobToFinish() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val controller = MonitoringJobController(scope)
        var activeJobs = 0
        var maximumActiveJobs = 0
        var startedJobs = 0

        val block: suspend () -> Unit = {
            startedJobs++
            activeJobs++
            maximumActiveJobs = maxOf(maximumActiveJobs, activeJobs)
            try {
                awaitCancellation()
            } finally {
                activeJobs--
            }
        }

        controller.start(block)
        runCurrent()
        assertEquals(1, startedJobs)
        assertEquals(1, activeJobs)

        controller.start(block)
        runCurrent()
        assertEquals(2, startedJobs)
        assertEquals(1, activeJobs)
        assertEquals(1, maximumActiveJobs)

        controller.stop()
        runCurrent()
        assertEquals(0, activeJobs)
        scope.cancel()
    }
}
