package com.example.supervisor_client.collectors.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MessageNotificationParserTest {
    @Test
    fun acceptsNotificationsOnlyFromTheDefaultSmsApp() {
        assertEquals(
            "Jordan Smith",
            MessageNotificationParser.senderFromNotification(
                notificationPackage = "sms.app",
                defaultSmsPackage = "sms.app",
                title = StringBuilder("  Jordan Smith  ")
            )
        )
    }

    @Test
    fun ignoresNotificationsFromOtherAppsOrWhenNoSmsAppIsDefault() {
        assertNull(
            MessageNotificationParser.senderFromNotification(
                notificationPackage = "other.app",
                defaultSmsPackage = "sms.app",
                title = "Jordan Smith"
            )
        )
        assertNull(
            MessageNotificationParser.senderFromNotification(
                notificationPackage = "sms.app",
                defaultSmsPackage = null,
                title = "Jordan Smith"
            )
        )
    }

    @Test
    fun usesOnlyTheNotificationTitleAsTheSender() {
        assertEquals(
            "Jordan Smith",
            MessageNotificationParser.senderFromTitle("  Jordan Smith  ")
        )
    }

    @Test
    fun ignoresMissingOrBlankNotificationTitles() {
        assertNull(MessageNotificationParser.senderFromTitle(null))
        assertNull(MessageNotificationParser.senderFromTitle("  "))
    }

    @Test
    fun createsLogEntryWithOnlyTheSenderAndNotificationTimestamp() {
        val log = MessageNotificationParser.logEntryFromNotification(
            notificationPackage = "sms.app",
            defaultSmsPackage = "sms.app",
            title = "  Jordan Smith  ",
            timestamp = 123_456L
        )

        assertNotNull(log)
        assertEquals("Jordan Smith", log?.sender)
        assertEquals(123_456L, log?.timestamp)
    }

    @Test
    fun doesNotCreateLogEntriesForNotificationsFromOtherApps() {
        assertNull(
            MessageNotificationParser.logEntryFromNotification(
                notificationPackage = "other.app",
                defaultSmsPackage = "sms.app",
                title = "Jordan Smith",
                timestamp = 123_456L
            )
        )
    }
}
