package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity
data class SystemLogPlaceholderEntity(
    @PrimaryKey val id: Long = 0,
) {
    // TODO: Replace with story entities
}

@Database(
    entities = [SystemLogPlaceholderEntity::class, ContactEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "supervisor.db",
                ).build().also { instance = it }
            }
    }
}
