package com.example.supervisor_client.collectors.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.supervisor_client.collectors.DataCollector

class ContactsCollector(
    private val context: Context,
) : DataCollector {

    fun collectContacts(): List<ContactInfo> {

        // Check whether contacts permission has been granted.
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val contacts = mutableListOf<ContactInfo>()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->

            val nameIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            val numberIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex) ?: ""
                val number = cursor.getString(numberIndex) ?: ""

                contacts.add(
                    ContactInfo(name, number)
                )
            }
        }

        return contacts
    }
}
