package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageNotificationLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MessageLogDatabase : RoomDatabase() {
    abstract fun messageNotificationLogDao(): MessageNotificationLogDao

    companion object {
        const val DATABASE_NAME = "message_notification_logs.db"

        @Volatile
        private var instance: MessageLogDatabase? = null

        fun getInstance(context: Context): MessageLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MessageLogDatabase::class.java,
                    DATABASE_NAME
                ).build()
                    .also { instance = it }
            }
    }
}
