package com.example.supervisor_client

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.InstalledAppEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class InstalledAppsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_installed_apps)

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

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

    private fun displayApps(apps: List<InstalledAppEntity>) {

        val appList =
            findViewById<LinearLayout>(R.id.installedAppList)

        appList.removeAllViews()

        if (apps.isEmpty()) {

            val emptyText = TextView(this)

            emptyText.text = "No installed applications found."
            emptyText.textSize = 16f

            appList.addView(emptyText)

            return
        }

        for (app in apps) {

            val appText = TextView(this)

            val firstSeen =
                DateFormat.getDateTimeInstance()
                    .format(Date(app.firstSeen))

            val lastSeen =
                DateFormat.getDateTimeInstance()
                    .format(Date(app.lastSeen))

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
                24
            )

            appList.addView(appText)
        }
    }
}