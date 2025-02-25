package com.example.sms.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class AuthRepository(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).deviceTokenDao()
    private val db = AppDatabase.getDatabase(context)

    suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) { dao.getToken() }
    }

    suspend fun isTokenValid(token: String): Boolean {
        val url = "https://dripmoney.io/check-device-token/"
        val json = JSONObject().apply {
            put("device_token", token)
        }
        val requestBody = RequestBody.create("application/json".toMediaType(), json.toString())


        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                jsonResponse.optBoolean("exists", false)
            } catch (e: IOException) {
                false
            }
        }
    }
    suspend fun getDeviceToken(): String? {
        // Предполагаем, что в таблице есть строка с device_token, либо возвращаем null
        return db.deviceTokenDao().getToken()
    }
    suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            dao.saveToken(DeviceTokenEntity(token))
        }
    }

    suspend fun deleteToken() {
        withContext(Dispatchers.IO) {
            dao.deleteToken()
        }
    }
}
