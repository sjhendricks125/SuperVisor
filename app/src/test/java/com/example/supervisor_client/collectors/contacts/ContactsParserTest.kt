package com.example.supervisor_client.collectors.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsParserTest {

    @Test
    //UT-01
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
    //UT-02
    fun removesExtraWhitespace() {
        val contacts = listOf(
            ContactInfo("  Test Person  ", " 3175550100 "),
        )

        val result = ContactsParser.parse(contacts)

        assertEquals("Test Person", result[0].name)
        assertEquals("3175550100", result[0].phoneNumber)
    }
    //UT-03
    @Test
    fun skipsBlankPhoneNumber() {
        val contacts = listOf(
            ContactInfo("Test Person", ""),
        )

        val result = ContactsParser.parse(contacts)

        assertTrue(result.isEmpty())
    }
    //UT-04
    @Test
    fun skipsWhitespaceOnlyPhoneNumber() {
        val contacts = listOf(
            ContactInfo("Test Person", "   "),
        )

        val result = ContactsParser.parse(contacts)

        assertTrue(result.isEmpty())
    }
    //UT-05
    @Test
    fun handlesEmptyContactList() {
        val result = ContactsParser.parse(emptyList())

        assertTrue(result.isEmpty())
    }
    //UT-06
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
