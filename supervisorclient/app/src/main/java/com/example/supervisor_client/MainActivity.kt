package com.example.supervisor_client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.supervisor_client.collectors.battery.BatteryCollector
import com.example.supervisor_client.data.local.BatteryPingEntity
import com.example.supervisor_client.data.local.MonitoringConfigStore
import com.example.supervisor_client.databinding.ActivityMainBinding
import com.example.supervisor_client.services.MonitoringForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.Instant
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var monitoringEnabled = false

    private val batteryDao by lazy {
        (application as SuperVisorApp).database.batteryPingDao()
    }
    private val batteryCollector by lazy {
        BatteryCollector(this, batteryDao)
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.etIntervalMinutes.setText(
            MonitoringConfigStore.getPingIntervalMinutes(this).toString()
        )
        binding.btnGrantPermissions.setOnClickListener {
            permissionRequest.launch(requiredPermissions())
        }
        binding.btnTestPing.setOnClickListener {
            lifecycleScope.launch {
                binding.btnTestPing.isEnabled = false
                try {
                    val ping = batteryCollector.collectAndStorePing()
                    refreshStatus(
                        "Last ping: ${ping.status} | Battery: ${ping.levelPercent ?: "N/A"}%"
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    refreshStatus("Ping failed: ${exception.message ?: "unknown error"}")
                } finally {
                    if (!isDestroyed) {
                        binding.btnTestPing.isEnabled = true
                    }
                }
            }
        }
        binding.btnToggleService.setOnClickListener {
            toggleMonitoringService()
        }

        lifecycleScope.launch {
            batteryDao.getAllPingsFlow().collect { pings ->
                binding.tvBatteryLogs.text = pings.joinToString(separator = "\n\n") {
                    formatBatteryPing(it)
                }.ifEmpty { "No battery pings yet." }
            }
        }
        refreshStatus()
        updateMonitoringButton()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            monitoringEnabled = MonitoringForegroundService.isMonitoring
            updateMonitoringButton()
            if (!binding.etIntervalMinutes.hasFocus()) {
                binding.etIntervalMinutes.setText(
                    MonitoringConfigStore.getPingIntervalMinutes(this).toString()
                )
            }
            refreshStatus()
        }
    }

    // Battery needs no permissions; only notifications (Android 13+) for the foreground service
    private fun requiredPermissions(): Array<String> =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    private fun toggleMonitoringService() {
        if (monitoringEnabled) {
            startService(
                Intent(this, MonitoringForegroundService::class.java)
                    .setAction(MonitoringForegroundService.ACTION_STOP_MONITORING)
            )
            monitoringEnabled = false
            updateMonitoringButton()
            refreshStatus("Scheduled monitoring stopped.")
            return
        }

        val intervalMinutes = binding.etIntervalMinutes.text
            .toString()
            .trim()
            .toLongOrNull()
        if (intervalMinutes == null || intervalMinutes !in 1L..10_080L) {
            binding.etIntervalMinutes.error = "Enter an interval from 1 to 10080 minutes."
            return
        }
        binding.etIntervalMinutes.error = null

        try {
            MonitoringForegroundService.startOrUpdateSchedule(this, intervalMinutes)
            monitoringEnabled = true
            updateMonitoringButton()
            refreshStatus("Scheduled monitoring started.")
        } catch (exception: Exception) {
            monitoringEnabled = false
            updateMonitoringButton()
            refreshStatus("Unable to start monitoring: ${exception.message ?: "unknown error"}")
        }
    }

    private fun updateMonitoringButton() {
        binding.btnToggleService.text = if (monitoringEnabled) {
            "Stop Scheduled Monitoring"
        } else {
            "Start Scheduled Monitoring"
        }
    }

    private fun refreshStatus(message: String? = null) {
        val notificationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            "Notifications: ${if (granted) "granted" else "not granted"}"
        } else {
            "Notifications: not required"
        }
        val detail = message?.let { "\n$it" } ?: ""
        binding.tvPermissionStatus.text = "$notificationStatus$detail"
    }

    private fun formatBatteryPing(ping: BatteryPingEntity): String {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(ping.timestamp)
        )
        val level = ping.levelPercent?.let { "$it%" } ?: "N/A"
        val charging = when (ping.isCharging) {
            true -> "Charging (${ping.plugType})"
            false -> "Not charging"
            null -> "N/A"
        }
        val temp = ping.temperatureCelsius?.let { "%.1f°C".format(it) } ?: "N/A"
        return "ID: ${ping.id} | UTC: $timestamp\n" +
                "Level: $level (${ping.levelCategory}) | $charging\n" +
                "Health: ${ping.health ?: "N/A"} | Temp: $temp | " +
                "Power save: ${ping.isPowerSaveMode ?: "N/A"}\n" +
                "Status: ${ping.status} | Synced: ${ping.isSynced}"
    }
}