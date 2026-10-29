package com.example.supervisor_client

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.supervisor_client.data.local.AppChangeEventEntity
import com.example.supervisor_client.data.local.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class AppChangesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_changes)

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        observeAppChanges()
    }

    private fun observeAppChanges() {

        val database = AppDatabase.getInstance(this)

        lifecycleScope.launch {

            database.appChangeEventDao()
                .getAllEventsFlow()
                .collectLatest { events ->

                    displayEvents(events)
                }
        }
    }

    private fun displayEvents(
        events: List<AppChangeEventEntity>
    ) {

        val eventList =
            findViewById<LinearLayout>(R.id.appChangeList)

        eventList.removeAllViews()

        if (events.isEmpty()) {

            val emptyText = TextView(this)

            emptyText.text = "No application changes recorded."
            emptyText.textSize = 16f

            eventList.addView(emptyText)

            return
        }

        for (event in events) {

            val eventText = TextView(this)

            val timestamp =
                DateFormat.getDateTimeInstance()
                    .format(Date(event.timestamp))

            eventText.text =
                "${event.eventType}\n" +
                        "App: ${event.appName ?: "Unknown"}\n" +
                        "Package: ${event.packageName}\n" +
                        "Version: ${event.versionName ?: "Unknown"}\n" +
                        "System App: ${event.isSystemApp ?: "Unknown"}\n" +
                        "Time: $timestamp"

            eventText.textSize = 16f

            eventText.setPadding(
                0,
                12,
                0,
                24
            )

            eventList.addView(eventText)
        }
    }
}