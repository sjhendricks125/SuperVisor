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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallAppDaoTest {

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
    fun insertAppCanBeRetrieved() = runBlocking {

        val dao = database.installedAppDao()

        val app = InstalledAppEntity(
            packageName = "com.facebook.katana",
            appName = "Facebook",
            versionName = "123.0",
            isSystemApp = false,
            firstSeen = 1000L,
            lastSeen = 1000L
        )

        dao.insertOrUpdateApp(app)

        val storedApp = dao.getApp("com.facebook.katana")

        assertNotNull(storedApp)
        assertEquals("Facebook", storedApp?.appName)
        assertEquals("com.facebook.katana", storedApp?.packageName)
        assertEquals("123.0", storedApp?.versionName)
        assertEquals(false, storedApp?.isSystemApp)
    }

    @Test
    fun firstSeenTimestampIsPreservedWhenAppIsUpdated() = runBlocking {

        val dao = database.installedAppDao()

        val firstVersion = InstalledAppEntity(
            packageName = "com.facebook.katana",
            appName = "Facebook",
            versionName = "123.0",
            isSystemApp = false,
            firstSeen = 1000L,
            lastSeen = 1000L
        )

        dao.insertOrUpdateApp(firstVersion)

        val updatedVersion = InstalledAppEntity(
            packageName = "com.facebook.katana",
            appName = "Facebook",
            versionName = "124.0",
            isSystemApp = false,
            firstSeen = 1000L,
            lastSeen = 2000L
        )

        dao.insertOrUpdateApp(updatedVersion)

        val storedApp = dao.getApp("com.facebook.katana")

        assertEquals(1000L, storedApp?.firstSeen)
        assertEquals(2000L, storedApp?.lastSeen)
        assertEquals("124.0", storedApp?.versionName)
    }

    @Test
    fun appsAreReturnedInAlphabeticalOrder() = runBlocking {

        val dao = database.installedAppDao()

        dao.insertOrUpdateApp(
            app(
                packageName = "com.zeta.app",
                appName = "Zeta"
            )
        )

        dao.insertOrUpdateApp(
            app(
                packageName = "com.alpha.app",
                appName = "Alpha"
            )
        )

        dao.insertOrUpdateApp(
            app(
                packageName = "com.beta.app",
                appName = "Beta"
            )
        )

        val apps = dao.getAllApps()

        assertEquals(
            listOf("Alpha", "Beta", "Zeta"),
            apps.map { it.appName }
        )
    }

    @Test
    fun getAllAppsFlowReturnsInsertedApps() = runBlocking {

        val dao = database.installedAppDao()

        dao.insertOrUpdateApp(
            app(
                packageName = "com.facebook.katana",
                appName = "Facebook"
            )
        )

        dao.insertOrUpdateApp(
            app(
                packageName = "com.google.android.youtube",
                appName = "YouTube"
            )
        )

        val apps = dao.getAllAppsFlow().first()

        assertEquals(
            listOf("Facebook", "YouTube"),
            apps.map { it.appName }
        )
    }

    @Test
    fun deleteAppRemovesOnlySpecifiedApp() = runBlocking {

        val dao = database.installedAppDao()

        dao.insertOrUpdateApp(
            app(
                packageName = "com.facebook.katana",
                appName = "Facebook"
            )
        )

        dao.insertOrUpdateApp(
            app(
                packageName = "com.google.android.youtube",
                appName = "YouTube"
            )
        )

        dao.deleteApp("com.facebook.katana")

        assertNull(
            dao.getApp("com.facebook.katana")
        )

        assertNotNull(
            dao.getApp("com.google.android.youtube")
        )
    }

    @Test
    fun updateLastSeenChangesOnlyLastSeenTimestamp() = runBlocking {

        val dao = database.installedAppDao()

        dao.insertOrUpdateApp(
            InstalledAppEntity(
                packageName = "com.facebook.katana",
                appName = "Facebook",
                versionName = "123.0",
                isSystemApp = false,
                firstSeen = 1000L,
                lastSeen = 1000L
            )
        )

        dao.updateLastSeen(
            packageName = "com.facebook.katana",
            timestamp = 5000L
        )

        val storedApp = dao.getApp("com.facebook.katana")

        assertEquals(1000L, storedApp?.firstSeen)
        assertEquals(5000L, storedApp?.lastSeen)
    }

    private fun app(
        packageName: String,
        appName: String
    ) = InstalledAppEntity(
        packageName = packageName,
        appName = appName,
        versionName = "1.0",
        isSystemApp = false,
        firstSeen = 1000L,
        lastSeen = 1000L
    )
}