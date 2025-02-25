package com.example.sms.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_token")
data class DeviceTokenEntity(
    @PrimaryKey val token: String
)

