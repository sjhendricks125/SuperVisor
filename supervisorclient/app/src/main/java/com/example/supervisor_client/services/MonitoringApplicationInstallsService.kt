package com.example.supervisor_client.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.supervisor_client.data.local.AppChangeEventEntity
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.InstalledAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MonitoringApplicationInstallsService : Service() {

    private val TAG = "MonitoringAppInstallsService"

    private var appChangeReceiver: BroadcastReceiver? = null

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        startForegroundServiceWithNotification()
        registerAppChangeReceiver()
    }

    private fun startForegroundServiceWithNotification() {

        val channelId = "monitoring_app_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "App Installation Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager?.createNotificationChannel(channel)
        }

        val notification: Notification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("Supervisor App Tracking")
                .setContentText(
                    "Monitoring application installs and uninstalls."
                )
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()

        startForeground(2, notification)
    }

    private fun registerAppChangeReceiver() {

        appChangeReceiver = object : BroadcastReceiver() {

            override fun onReceive(
                context: Context,
                intent: Intent
            ) {

                val packageName =
                    intent.data?.schemeSpecificPart ?: return

                val packageManager = context.packageManager

                when (intent.action) {

                    Intent.ACTION_PACKAGE_ADDED -> {

                        val replacing =
                            intent.getBooleanExtra(
                                Intent.EXTRA_REPLACING,
                                false
                            )

                        if (!replacing) {

                            val appInfo = try {

                                val applicationInfo =
                                    packageManager.getApplicationInfo(
                                        packageName,
                                        0
                                    )

                                val appName =
                                    packageManager
                                        .getApplicationLabel(
                                            applicationInfo
                                        )
                                        .toString()

                                val packageInfo =
                                    packageManager.getPackageInfo(
                                        packageName,
                                        0
                                    )

                                val versionName =
                                    packageInfo.versionName ?: "Unknown"

                                val isSystemApp =
                                    (
                                            applicationInfo.flags
                                                    and android.content.pm.ApplicationInfo.FLAG_SYSTEM
                                            ) != 0

                                Triple(
                                    appName,
                                    versionName,
                                    isSystemApp
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Unable to get installed app information",
                                    e
                                )

                                Triple(
                                    "Unknown",
                                    "Unknown",
                                    false
                                )
                            }

                            val appName = appInfo.first
                            val versionName = appInfo.second
                            val isSystemApp = appInfo.third

                            val timestamp = System.currentTimeMillis()

                            val readableTimestamp =
                                DateFormat.getDateTimeInstance().format(
                                    Date(timestamp)
                                )

                            Log.d(
                                TAG,
                                "INSTALLED: $appName"
                            )

                            Log.d(
                                TAG,
                                "Package Name: $packageName"
                            )

                            Log.d(
                                TAG,
                                "Version: $versionName | " +
                                        "System App: $isSystemApp"
                            )

                            Log.d(
                                TAG,
                                "Timestamp: $timestamp ($readableTimestamp)"
                            )

                            saveInstallEvent(
                                packageName = packageName,
                                appName = appName,
                                versionName = versionName,
                                isSystemApp = isSystemApp
                            )
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {

                        val replacing =
                            intent.getBooleanExtra(
                                Intent.EXTRA_REPLACING,
                                false
                            )

                        if (!replacing) {

                            val timestamp = System.currentTimeMillis()

                            val readableTimestamp =
                                DateFormat.getDateTimeInstance().format(
                                    Date(timestamp)
                                )

                            Log.d(
                                TAG,
                                "UNINSTALLED: $packageName"
                            )

                            Log.d(
                                TAG,
                                "Timestamp: $timestamp ($readableTimestamp)"
                            )

                            saveUninstallEvent(
                                packageName = packageName
                            )
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                appChangeReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )

        } else {

            registerReceiver(
                appChangeReceiver,
                filter
            )
        }
    }

    private fun saveInstallEvent(
        packageName: String,
        appName: String,
        versionName: String,
        isSystemApp: Boolean
    ) {

        serviceScope.launch {

            val database =
                AppDatabase.getInstance(applicationContext)

            val appDao = database.installedAppDao()
            val eventDao = database.appChangeEventDao()

            val timestamp = System.currentTimeMillis()

            val existingApp =
                appDao.getApp(packageName)

            val appEntity = InstalledAppEntity(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                isSystemApp = isSystemApp,
                firstSeen = existingApp?.firstSeen ?: timestamp,
                lastSeen = timestamp
            )

            appDao.insertOrUpdateApp(appEntity)

            eventDao.insertEvent(
                AppChangeEventEntity(
                    timestamp = timestamp,
                    packageName = packageName,
                    appName = appName,
                    versionName = versionName,
                    isSystemApp = isSystemApp,
                    eventType = "INSTALLED"
                )
            )

            val readableTimestamp =
                DateFormat.getDateTimeInstance().format(
                    Date(timestamp)
                )

            Log.d(
                TAG,
                "Saved install event to database: $packageName"
            )

            Log.d(
                TAG,
                "Database Timestamp: $timestamp ($readableTimestamp)"
            )
        }
    }

    private fun saveUninstallEvent(
        packageName: String
    ) {

        serviceScope.launch {

            val database =
                AppDatabase.getInstance(applicationContext)

            val appDao =
                database.installedAppDao()

            val eventDao =
                database.appChangeEventDao()

            val timestamp =
                System.currentTimeMillis()

            val existingApp =
                appDao.getApp(packageName)

            eventDao.insertEvent(
                AppChangeEventEntity(
                    timestamp = timestamp,
                    packageName = packageName,
                    appName = existingApp?.appName,
                    versionName = existingApp?.versionName,
                    isSystemApp = existingApp?.isSystemApp,
                    eventType = "UNINSTALLED"
                )
            )

            appDao.deleteApp(packageName)

            val readableTimestamp =
                DateFormat.getDateTimeInstance().format(
                    Date(timestamp)
                )

            Log.d(
                TAG,
                "Saved uninstall event to database: $packageName"
            )

            Log.d(
                TAG,
                "Database Timestamp: $timestamp ($readableTimestamp)"
            )
        }
    }

    override fun onDestroy() {

        appChangeReceiver?.let {

            try {
                unregisterReceiver(it)

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error unregistering receiver",
                    e
                )
            }
        }

        serviceScope.cancel()

        super.onDestroy()
    }
}