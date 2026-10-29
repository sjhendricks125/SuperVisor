package com.example.supervisor_client

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.supervisor_client.collectors.apps.AppInstallCollectorStub
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.InstalledAppEntity
import com.example.supervisor_client.services.MonitoringApplicationInstallsService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainView = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        AppInstallCollectorStub(this).logAllInstalledApps()

        val serviceIntent =
            android.content.Intent(this, MonitoringApplicationInstallsService::class.java)

        startForegroundService(serviceIntent)

        observeInstalledApps()
    }

    private fun observeInstalledApps() {

        val database = AppDatabase.getInstance(this)

        lifecycleScope.launch {

            database.installedAppDao()
                .getAllAppsFlow()
                .collectLatest { apps ->

                    displayApps(apps)
                }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayApps(apps: List<InstalledAppEntity>) {

        val appList = findViewById<android.widget.LinearLayout>(R.id.appList)

        // Keep the "Installed Applications" title.
        // Remove everything after it.
        if (appList.childCount > 1) {
            appList.removeViews(1, appList.childCount - 1)
        }

        for (app in apps) {

            val appText = TextView(this)

            val firstSeen = DateFormat.getDateTimeInstance().format(
                Date(app.firstSeen)
            )

            val lastSeen = DateFormat.getDateTimeInstance().format(
                Date(app.lastSeen)
            )

            appText.text =
                "${app.appName}\n" +
                        "${app.packageName}\n" +
                        "Version: ${app.versionName}\n" +
                        "System App: ${app.isSystemApp}\n" +
                        "First Seen: $firstSeen\n" +
                        "Last Seen: $lastSeen"

            appText.textSize = 16f

            appText.setPadding(
                0,
                12,
                0,
                20
            )

            appList.addView(appText)
        }
    }
}