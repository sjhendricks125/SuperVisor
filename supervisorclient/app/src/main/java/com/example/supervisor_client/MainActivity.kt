package com.example.supervisor_client

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.supervisor_client.collectors.apps.AppInstallCollectorStub
import com.example.supervisor_client.services.MonitoringApplicationInstallsService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainView = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

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
            Intent(this, MonitoringApplicationInstallsService::class.java)

        startForegroundService(serviceIntent)

        findViewById<android.widget.Button>(R.id.viewInstalledAppsButton)
            .setOnClickListener {

                val intent = Intent(
                    this,
                    InstalledAppsActivity::class.java
                )

                startActivity(intent)
            }

        findViewById<android.widget.Button>(R.id.viewAppChangesButton)
            .setOnClickListener {

                val intent = Intent(
                    this,
                    AppChangesActivity::class.java
                )

                startActivity(intent)
            }
    }
}