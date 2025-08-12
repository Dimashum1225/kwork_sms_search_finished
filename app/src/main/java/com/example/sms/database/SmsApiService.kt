package com.example.sms.database

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SmsApiService {

    @POST("confirm_payment/")  // Убедитесь, что URL правильный
    suspend fun sendSms(@Body sms: SmsData): Response<Unit>
}
