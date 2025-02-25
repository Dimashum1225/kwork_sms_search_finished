package com.example.sms.database

data class SmsData(
    val sender: String,
    val text: String,
    val device_token: String
)
