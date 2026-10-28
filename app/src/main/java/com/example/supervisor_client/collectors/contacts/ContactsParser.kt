package com.example.supervisor_client.collectors.contacts

import com.example.supervisor_client.data.local.ContactEntity

object ContactsParser {

    fun parse(contacts: List<ContactInfo>): List<ContactEntity> {

        return contacts.mapNotNull { contact ->

            val name = contact.name.trim()
            val number = contact.phoneNumber.trim()

            if (number.isBlank()) {
                null
            } else {
                ContactEntity(
                    name = name,
                    phoneNumber = number
                )
            }
        }
    }
}
