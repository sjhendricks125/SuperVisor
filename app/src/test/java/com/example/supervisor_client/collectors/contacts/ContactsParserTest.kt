package com.example.supervisor_client.collectors.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsParserTest {

    @Test
    fun parsesValidContact() {
        val contacts = listOf(
            ContactInfo("Test Person", "3175550100"),
        )

        val result = ContactsParser.parse(contacts)

        assertEquals(1, result.size)
        assertEquals("Test Person", result[0].name)
        assertEquals("3175550100", result[0].phoneNumber)
    }

    @Test
    fun removesExtraWhitespace() {
        val contacts = listOf(
            ContactInfo("  Test Person  ", " 3175550100 "),
        )

        val result = ContactsParser.parse(contacts)

        assertEquals("Test Person", result[0].name)
        assertEquals("3175550100", result[0].phoneNumber)
    }

    @Test
    fun skipsBlankPhoneNumber() {
        val contacts = listOf(
            ContactInfo("Test Person", ""),
        )

        val result = ContactsParser.parse(contacts)

        assertTrue(result.isEmpty())
    }

    @Test
    fun skipsWhitespaceOnlyPhoneNumber() {
        val contacts = listOf(
            ContactInfo("Test Person", "   "),
        )

        val result = ContactsParser.parse(contacts)

        assertTrue(result.isEmpty())
    }

    @Test
    fun handlesEmptyContactList() {
        val result = ContactsParser.parse(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun parsesMultipleContacts() {
        val contacts = listOf(
            ContactInfo("Person One", "1115550100"),
            ContactInfo("Person Two", "2225550100"),
            ContactInfo("Person Three", "3335550100"),
        )

        val result = ContactsParser.parse(contacts)

        assertEquals(3, result.size)
        assertEquals("Person One", result[0].name)
        assertEquals("Person Two", result[1].name)
        assertEquals("Person Three", result[2].name)
    }
}
