package com.example.sms.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class AuthRepository(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).deviceTokenDao()
    private val db = AppDatabase.getDatabase(context)

    private val TAG = "AuthRepository"

    suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            val token = dao.getToken()
            Log.d(TAG, "getToken: $token")
            token
        }
    }

    suspend fun isTokenValid(token: String): Boolean {
        val url = "https://flpay.tech/api/check-api-key/"
        val json = JSONObject().apply {
            put("api_key", token)
        }
        val requestBody = RequestBody.create("application/json".toMediaType(), json.toString())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Sending request to $url with token: $token")
                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d(TAG, "Response body: $responseBody")
                val jsonResponse = JSONObject(responseBody ?: "{}")
                val success = jsonResponse.optBoolean("success", false)
                Log.d(TAG, "Parsed success: $success")
                success
            } catch (e: IOException) {
                Log.e(TAG, "Network error: ${e.message}", e)
                false
            }
        }
    }

    suspend fun getDeviceToken(): String? {
        return withContext(Dispatchers.IO) {
            val token = db.deviceTokenDao().getToken()
            Log.d(TAG, "getDeviceToken: $token")
            token
        }
    }

    suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Saving token: $token")
            dao.saveToken(DeviceTokenEntity(token))
        }
    }

    suspend fun deleteToken() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Deleting token")
            dao.deleteToken()
        }
    }
}
