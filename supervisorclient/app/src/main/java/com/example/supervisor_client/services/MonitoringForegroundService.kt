package com.example.supervisor_client.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MonitoringForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    // TODO: Implement foreground monitoring behavior.
}
