package com.example.sms.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sms.logs.SmsLogDao
import com.example.sms.logs.SmsLogEntity

@Database(entities = [DeviceTokenEntity::class, SmsLogEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceTokenDao(): DeviceTokenDao
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
