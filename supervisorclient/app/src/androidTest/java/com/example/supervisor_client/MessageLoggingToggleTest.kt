package com.example.supervisor_client

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.supervisor_client.data.local.MonitoringConfigStore
import com.google.android.material.switchmaterial.SwitchMaterial
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageLoggingToggleTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var originalMessageLoggingEnabled: Boolean? = null

    @Before
    fun disableMessageLoggingForUiTest() {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        originalMessageLoggingEnabled = if (
            preferences.contains(KEY_MESSAGE_NOTIFICATION_LOGGING_ENABLED)
        ) {
            MonitoringConfigStore.isMessageNotificationLoggingEnabled(context)
        } else {
            null
        }
        MonitoringConfigStore.setMessageNotificationLoggingEnabled(context, false)
    }

    @After
    fun restoreMessageLoggingPreference() {
        if (originalMessageLoggingEnabled == null) {
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_MESSAGE_NOTIFICATION_LOGGING_ENABLED)
                .apply()
        } else {
            MonitoringConfigStore.setMessageNotificationLoggingEnabled(
                context,
                originalMessageLoggingEnabled == true
            )
        }
    }

    @Test
    fun switchReflectsNotificationAccessAndTogglesLoggingWhenAvailable() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                val messageLoggingSwitch =
                    activity.findViewById<SwitchMaterial>(R.id.switchMessageLogging)
                val messageLoggingStatus =
                    activity.findViewById<TextView>(R.id.tvMessageLoggingStatus)

                assertFalse(messageLoggingSwitch.isChecked)
                assertFalse(MonitoringConfigStore.isMessageNotificationLoggingEnabled(context))
                val disabledStatus = messageLoggingStatus.text.toString()
                if (disabledStatus == context.getString(R.string.message_notification_logging_off)) {
                    return@onActivity
                }
                assertEquals(
                    context.getString(R.string.notification_logging_off_but_access_granted),
                    disabledStatus
                )

                messageLoggingSwitch.performClick()

                assertTrue(messageLoggingSwitch.isChecked)
                assertEquals(
                    context.getString(R.string.message_notification_logging_on),
                    messageLoggingStatus.text.toString()
                )
                assertTrue(MonitoringConfigStore.isMessageNotificationLoggingEnabled(context))

                messageLoggingSwitch.performClick()

                assertFalse(messageLoggingSwitch.isChecked)
                assertEquals(
                    context.getString(R.string.notification_logging_off_but_access_granted),
                    messageLoggingStatus.text.toString()
                )
                assertFalse(MonitoringConfigStore.isMessageNotificationLoggingEnabled(context))
            }
        } finally {
            scenario.close()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "supervisor_config"
        const val KEY_MESSAGE_NOTIFICATION_LOGGING_ENABLED =
            "message_notification_logging_enabled"
    }
}
