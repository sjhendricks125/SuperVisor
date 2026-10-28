package com.example.supervisor_client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactDao {

    @Insert
    suspend fun insertContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<ContactEntity>
}
