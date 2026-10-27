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
import com.example.supervisor_client.collectors.contacts.ContactsCollectorStub
import com.example.supervisor_client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val contactsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
    { granted ->
        if (granted) {
            val collector = ContactsCollectorStub(this)
            val contacts = collector.collectContacts()

            Log.d(
                "SuperVisor",
                "Contacts retrieve: ${contacts.size}"
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val collector = ContactsCollectorStub(this)
            val contacts = collector.collectContacts()

            Log.d("SuperVisor", "Contacts retrieved: ${contacts.size}")
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