package com.example.sms.logs

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val sms_text: String,
    val timestamp: Long
)

