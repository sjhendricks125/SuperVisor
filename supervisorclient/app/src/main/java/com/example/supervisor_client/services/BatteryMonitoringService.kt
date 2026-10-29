package com.example.supervisor_client.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.supervisor_client.R
import com.example.supervisor_client.SuperVisorApp
import com.example.supervisor_client.collectors.battery.BatteryCollector
import com.example.supervisor_client.data.local.BatteryConfigStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BatteryMonitoringService : Service() {
    companion object {
        private const val TAG = "BatteryMonitoringService"

        const val ACTION_START_MONITORING =
            "com.example.supervisor_client.action.START_BATTERY_MONITORING"
        const val ACTION_STOP_MONITORING =
            "com.example.supervisor_client.action.STOP_BATTERY_MONITORING"
        const val EXTRA_INTERVAL_MINUTES = "extra_battery_interval_minutes"
        const val NOTIFICATION_CHANNEL_ID = "supervisor_battery_channel"
        const val NOTIFICATION_ID = 1002

        @Volatile
        var isMonitoring = false
            private set

        fun startOrUpdateSchedule(context: Context, intervalMinutes: Long) {
            val safeInterval = intervalMinutes.coerceAtLeast(1L)
            BatteryConfigStore.setPingIntervalMinutes(context, safeInterval)
            val intent = Intent(context, BatteryMonitoringService::class.java)
                .setAction(ACTION_START_MONITORING)
                .putExtra(EXTRA_INTERVAL_MINUTES, safeInterval)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null

    private val batteryCollector by lazy {
        val dao = (application as SuperVisorApp).database.batteryPingDao()
        BatteryCollector(applicationContext, dao)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            monitoringJob?.cancel()
            monitoringJob = null
            isMonitoring = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SuperVisor Active")
            .setContentText("Periodic battery monitoring enabled")
            .setOngoing(true)
            .build()

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } catch (_: Exception) {
            isMonitoring = false
            stopSelf()
            return START_NOT_STICKY
        }

        val intervalMinutes = intent
            ?.getLongExtra(EXTRA_INTERVAL_MINUTES, 0L)
            ?.takeIf { it >= 1L }
            ?: BatteryConfigStore.getPingIntervalMinutes(this)
        BatteryConfigStore.setPingIntervalMinutes(this, intervalMinutes)

        val previousJob = monitoringJob
        previousJob?.cancel()
        monitoringJob = serviceScope.launch {
            previousJob?.join()
            while (isActive) {
                try {
                    batteryCollector.collectAndStorePing()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    Log.e(TAG, "Battery ping collection failed", exception)
                }
                val currentInterval = BatteryConfigStore
                    .getPingIntervalMinutes(this@BatteryMonitoringService)
                    .coerceAtLeast(1L)
                val delayMillis = currentInterval
                    .coerceAtMost(Long.MAX_VALUE / 60_000L) * 60_000L
                delay(delayMillis)
            }
        }
        isMonitoring = true
        return START_STICKY
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        monitoringJob = null
        serviceScope.cancel()
        isMonitoring = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Battery monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}