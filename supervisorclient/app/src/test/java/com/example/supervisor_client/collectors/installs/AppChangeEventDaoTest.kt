package com.example.supervisor_client.data.local

import android.content.Context

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppChangeEventDaoTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {

        context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {

        database.close()
    }

    @Test
    fun insertInstallEventCanBeRetrieved() = runBlocking {

        val dao = database.appChangeEventDao()

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 1000L,
                packageName = "com.facebook.katana",
                appName = "Facebook",
                versionName = "123.0",
                isSystemApp = false,
                eventType = "INSTALLED"
            )
        )

        val events = dao.getAllEvents()

        assertEquals(1, events.size)
        assertEquals("Facebook", events.first().appName)
        assertEquals("com.facebook.katana", events.first().packageName)
        assertEquals("INSTALLED", events.first().eventType)
        assertEquals(1000L, events.first().timestamp)
    }

    @Test
    fun installAndUninstallEventsAreBothStored() = runBlocking {

        val dao = database.appChangeEventDao()

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 1000L,
                packageName = "com.facebook.katana",
                appName = "Facebook",
                versionName = "123.0",
                isSystemApp = false,
                eventType = "INSTALLED"
            )
        )

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 2000L,
                packageName = "com.facebook.katana",
                appName = "Facebook",
                versionName = "123.0",
                isSystemApp = false,
                eventType = "UNINSTALLED"
            )
        )

        val events = dao.getAllEvents()

        assertEquals(2, events.size)
        assertEquals(
            listOf("UNINSTALLED", "INSTALLED"),
            events.map { it.eventType }
        )
    }

    @Test
    fun eventsAreReturnedNewestFirst() = runBlocking {

        val dao = database.appChangeEventDao()

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 1000L,
                packageName = "com.example.old",
                appName = "Old App",
                versionName = "1.0",
                isSystemApp = false,
                eventType = "INSTALLED"
            )
        )

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 3000L,
                packageName = "com.example.new",
                appName = "New App",
                versionName = "1.0",
                isSystemApp = false,
                eventType = "INSTALLED"
            )
        )

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 2000L,
                packageName = "com.example.middle",
                appName = "Middle App",
                versionName = "1.0",
                isSystemApp = false,
                eventType = "INSTALLED"
            )
        )

        val events = dao.getAllEvents()

        assertEquals(
            listOf(3000L, 2000L, 1000L),
            events.map { it.timestamp }
        )
    }

    @Test
    fun eventFlowReturnsDatabaseChanges() = runBlocking {

        val dao = database.appChangeEventDao()

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 1000L,
                packageName = "com.facebook.katana",
                appName = "Facebook",
                versionName = "123.0",
                isSystemApp = false,
                eventType = "INSTALLED"
            )
        )

        val events = dao.getAllEventsFlow().first()

        assertEquals(1, events.size)
        assertEquals(
            "com.facebook.katana",
            events.first().packageName
        )
    }

    @Test
    fun uninstallEventCanStoreMissingAppInformation() = runBlocking {

        val dao = database.appChangeEventDao()

        dao.insertEvent(
            AppChangeEventEntity(
                timestamp = 5000L,
                packageName = "com.example.removed",
                appName = null,
                versionName = null,
                isSystemApp = null,
                eventType = "UNINSTALLED"
            )
        )

        val event = dao.getAllEvents().first()

        assertNotNull(event)
        assertEquals("UNINSTALLED", event.eventType)
        assertEquals(
            "com.example.removed",
            event.packageName
        )
        assertEquals(null, event.appName)
        assertEquals(null, event.versionName)
        assertEquals(null, event.isSystemApp)
    }
}