package com.example.sms.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceTokenDao {
    @Query("SELECT token FROM device_token LIMIT 1")
    suspend fun getToken(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveToken(token: DeviceTokenEntity)

    @Query("DELETE FROM device_token")
    suspend fun deleteToken()
}
