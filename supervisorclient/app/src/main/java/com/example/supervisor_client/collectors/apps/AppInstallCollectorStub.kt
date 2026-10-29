package com.example.supervisor_client.collectors.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.supervisor_client.collectors.DataCollector
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.InstalledAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

data class AppInfoModel(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val isSystemApp: Boolean
)

class AppInstallCollectorStub(private val context: Context) : DataCollector {

    private val TAG = "AppInstallCollector"

    private val collectorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    fun logAllInstalledApps() {

        collectorScope.launch {

            val packageManager = context.packageManager
            val database = AppDatabase.getInstance(context)
            val appDao = database.installedAppDao()

            val packages = packageManager.getInstalledPackages(0)
            val timestamp = System.currentTimeMillis()

            Log.d(
                TAG,
                "=== Starting Currently Installed Apps List (${packages.size} apps found) ==="
            )

            for (packageInfo in packages) {

                val appName =
                    packageInfo.applicationInfo
                        ?.loadLabel(packageManager)
                        ?.toString()
                        ?: packageInfo.packageName

                val versionName =
                    packageInfo.versionName ?: "Unknown"

                val isSystemApp =
                    ((packageInfo.applicationInfo?.flags ?: 0)
                            and ApplicationInfo.FLAG_SYSTEM) != 0

                val appInfo = AppInfoModel(
                    appName = appName,
                    packageName = packageInfo.packageName,
                    versionName = versionName,
                    isSystemApp = isSystemApp
                )

                val existingApp = appDao.getApp(appInfo.packageName)

                val entity = InstalledAppEntity(
                    packageName = appInfo.packageName,
                    appName = appInfo.appName,
                    versionName = appInfo.versionName,
                    isSystemApp = appInfo.isSystemApp,
                    firstSeen = existingApp?.firstSeen ?: timestamp,
                    lastSeen = timestamp
                )

                appDao.insertOrUpdateApp(entity)

                val firstSeenReadable = DateFormat.getDateTimeInstance().format(
                    Date(entity.firstSeen)
                )

                val lastSeenReadable = DateFormat.getDateTimeInstance().format(
                    Date(entity.lastSeen)
                )

                Log.d(
                    TAG,
                    "App: ${appInfo.appName} | " +
                            "Package: ${appInfo.packageName} | " +
                            "Version: ${appInfo.versionName} | " +
                            "System App: ${appInfo.isSystemApp}"
                )

                Log.d(
                    TAG,
                    "First Seen: ${entity.firstSeen} ($firstSeenReadable) | " +
                            "Last Seen: ${entity.lastSeen} ($lastSeenReadable)"
                )
            }

            Log.d(TAG, "=== End of Installed Apps List ===")
        }
    }
}