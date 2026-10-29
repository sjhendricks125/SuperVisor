package com.example.supervisor_client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.supervisor_client.collectors.battery.BatteryCollector // BATTERY
import com.example.supervisor_client.collectors.location.LocationCollectorStub
import com.example.supervisor_client.data.local.BatteryConfigStore // BATTERY
import com.example.supervisor_client.data.local.BatteryPingEntity // BATTERY
import com.example.supervisor_client.data.local.LocationPingEntity
import com.example.supervisor_client.data.local.MonitoringConfigStore
import com.example.supervisor_client.databinding.ActivityMainBinding
import com.example.supervisor_client.services.BatteryMonitoringService // BATTERY
import com.example.supervisor_client.services.MonitoringForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.time.Instant
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var monitoringEnabled = false
    private var batteryMonitoringEnabled = false // BATTERY

    private val locationDao by lazy {
        (application as SuperVisorApp).database.locationPingDao()
    }
    private val locationCollector by lazy {
        LocationCollectorStub(this, locationDao)
    }

    private val batteryDao by lazy {
        (application as SuperVisorApp).database.batteryPingDao()
    }
    private val batteryCollector by lazy {
        BatteryCollector(this, batteryDao)
    }

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionStatus()
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
        binding.etBatteryIntervalMinutes.setText(
            BatteryConfigStore.getPingIntervalMinutes(this).toString()
        )

        binding.btnGrantPermissions.setOnClickListener {
            permissionRequest.launch(requiredPermissions())
        }

        binding.btnTestPing.setOnClickListener {
            lifecycleScope.launch {
                binding.btnTestPing.isEnabled = false
                try {
                    val ping = locationCollector.collectAndStorePing()
                    refreshPermissionStatus("Last ping: ${ping.status}")
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    refreshPermissionStatus(
                        "Ping failed: ${exception.message ?: "unknown error"}"
                    )
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

        binding.btnBatteryTestPing.setOnClickListener {
            lifecycleScope.launch {
                binding.btnBatteryTestPing.isEnabled = false
                try {
                    val ping = batteryCollector.collectAndStorePing()
                    refreshBatteryStatus(
                        "Last ping: ${ping.status} | Battery: ${ping.levelPercent ?: "N/A"}%"
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    refreshBatteryStatus(
                        "Ping failed: ${exception.message ?: "unknown error"}"
                    )
                } finally {
                    if (!isDestroyed) {
                        binding.btnBatteryTestPing.isEnabled = true
                    }
                }
            }
        }
        binding.btnBatteryToggleService.setOnClickListener {
            toggleBatteryMonitoringService()
        }

        lifecycleScope.launch {
            locationDao.getAllPingsFlow().collect { pings ->
                binding.tvPingLogs.text = pings.joinToString(separator = "\n\n") {
                    formatPing(it)
                }.ifEmpty { "No location pings yet." }
            }
        }
        // BATTERY
        lifecycleScope.launch {
            batteryDao.getAllPingsFlow().collect { pings ->
                binding.tvBatteryLogs.text = pings.joinToString(separator = "\n\n") {
                    formatBatteryPing(it)
                }.ifEmpty { "No battery pings yet." }
            }
        }
        refreshPermissionStatus()
        updateMonitoringButton()
        updateBatteryMonitoringButton() // BATTERY
        refreshBatteryStatus() // BATTERY
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            monitoringEnabled = MonitoringForegroundService.isMonitoring
            batteryMonitoringEnabled = BatteryMonitoringService.isMonitoring // BATTERY
            updateMonitoringButton()
            updateBatteryMonitoringButton() // BATTERY
            if (!binding.etIntervalMinutes.hasFocus()) {
                binding.etIntervalMinutes.setText(
                    MonitoringConfigStore.getPingIntervalMinutes(this).toString()
                )
            }
            // BATTERY
            if (!binding.etBatteryIntervalMinutes.hasFocus()) {
                binding.etBatteryIntervalMinutes.setText(
                    BatteryConfigStore.getPingIntervalMinutes(this).toString()
                )
            }
            refreshPermissionStatus()
            refreshBatteryStatus()
        }
    }

    private fun requiredPermissions(): Array<String> =
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
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
            refreshPermissionStatus("Scheduled monitoring stopped.")
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

        if (!hasLocationPermission()) {
            refreshPermissionStatus("Grant location permissions before starting monitoring.")
            permissionRequest.launch(requiredPermissions())
            return
        }

        try {
            MonitoringForegroundService.startOrUpdateSchedule(this, intervalMinutes)
            monitoringEnabled = true
            updateMonitoringButton()
            refreshPermissionStatus("Scheduled monitoring started.")
        } catch (exception: Exception) {
            monitoringEnabled = false
            updateMonitoringButton()
            refreshPermissionStatus(
                "Unable to start monitoring: ${exception.message ?: "unknown error"}"
            )
        }
    }

    private fun updateMonitoringButton() {
        binding.btnToggleService.text = if (monitoringEnabled) {
            "Stop Scheduled Monitoring"
        } else {
            "Start Scheduled Monitoring"
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFinePermission || hasCoarsePermission
    }

    private fun refreshPermissionStatus(message: String? = null) {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val permissionStatus = when {
            hasFinePermission -> "Fine location granted"
            hasCoarsePermission -> "Approximate location granted"
            else -> "Not granted"
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsStatus = if (LocationManagerCompat.isLocationEnabled(locationManager)) {
            "Enabled"
        } else {
            "Disabled"
        }
        val notificationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            "\nNotifications: ${if (granted) "granted" else "not granted"}"
        } else {
            ""
        }
        val detail = message?.let { "\n$it" } ?: ""
        binding.tvPermissionStatus.text =
            "Location permission: $permissionStatus\nGPS: $gpsStatus$notificationStatus$detail"
    }

    private fun formatPing(ping: LocationPingEntity): String {
        val timestamp = DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(ping.timestamp)
        )
        val latitude = ping.latitude?.toString() ?: "N/A"
        val longitude = ping.longitude?.toString() ?: "N/A"
        val accuracy = ping.accuracyRadius?.let { "$it m" } ?: "N/A"
        return "ID: ${ping.id} | UTC: $timestamp\n" +
                "Lat/Lng: $latitude, $longitude | Accuracy: $accuracy\n" +
                "Signal: ${ping.signalQuality} | Status: ${ping.status} | Synced: ${ping.isSynced}"
    }

    private fun toggleBatteryMonitoringService() {
        if (batteryMonitoringEnabled) {
            startService(
                Intent(this, BatteryMonitoringService::class.java)
                    .setAction(BatteryMonitoringService.ACTION_STOP_MONITORING)
            )
            batteryMonitoringEnabled = false
            updateBatteryMonitoringButton()
            refreshBatteryStatus("Battery monitoring stopped.")
            return
        }

        val intervalMinutes = binding.etBatteryIntervalMinutes.text
            .toString()
            .trim()
            .toLongOrNull()
        if (intervalMinutes == null || intervalMinutes !in 1L..10_080L) {
            binding.etBatteryIntervalMinutes.error =
                "Enter an interval from 1 to 10080 minutes."
            return
        }
        binding.etBatteryIntervalMinutes.error = null

        try {
            BatteryMonitoringService.startOrUpdateSchedule(this, intervalMinutes)
            batteryMonitoringEnabled = true
            updateBatteryMonitoringButton()
            refreshBatteryStatus("Battery monitoring started.")
        } catch (exception: Exception) {
            batteryMonitoringEnabled = false
            updateBatteryMonitoringButton()
            refreshBatteryStatus(
                "Unable to start battery monitoring: ${exception.message ?: "unknown error"}"
            )
        }
    }

    private fun updateBatteryMonitoringButton() {
        binding.btnBatteryToggleService.text = if (batteryMonitoringEnabled) {
            "Stop Battery Monitoring"
        } else {
            "Start Battery Monitoring"
        }
    }

    private fun refreshBatteryStatus(message: String? = null) {
        val state = if (batteryMonitoringEnabled) "running" else "stopped"
        val detail = message?.let { "\n$it" } ?: ""
        binding.tvBatteryStatus.text = "Battery monitoring: $state$detail"
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