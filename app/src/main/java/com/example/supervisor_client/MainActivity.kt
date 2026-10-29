package com.example.supervisor_client
import android.content.pm.PackageManager

import android.os.Bundle
import android.util.Log
import android.Manifest
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.supervisor_client.collectors.contacts.ContactsCollector
import com.example.supervisor_client.collectors.contacts.ContactsParser
import com.example.supervisor_client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val contactsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
    { granted ->
        if (granted) {
            //FT-03
            val collector = ContactsCollector(this)
            val contacts = collector.collectContacts()
            //FT-04
            val parsedContacts = ContactsParser.parse(contacts)


            //logcat results ")
            Log.d(
                "SuperVisor",
                "Contacts retrieved: ${contacts.size}" +
                        "\nParsed contacts: ${parsedContacts.size}"

            )

        } else {
            Log.d(
                "SuperVisor",
                "Permission denied"
            )
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //FT-01
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val collector = ContactsCollector(this)
            val contacts = collector.collectContacts()
            val parsedContacts = ContactsParser.parse(contacts)

            Log.d("SuperVisor", "Contacts retrieved: ${contacts.size}")
            Log.d("SuperVisor", "Parsed contacts: ${parsedContacts.size}")
        } else {
            contactsPermissionLauncher.launch(
                Manifest.permission.READ_CONTACTS
            )
        }
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}