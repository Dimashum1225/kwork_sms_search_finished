package com.example.sms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

import com.example.sms.database.AppDatabase
import com.example.sms.database.AuthRepository
import com.example.sms.database.RetrofitInstance
import com.example.sms.database.SmsData
import com.example.sms.logs.SmsLogEntity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class LogNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotifLog", "Notification Listener Service connected")
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Проверка включённости отправки
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val shouldSend = prefs.getBoolean("shouldSendNotification", false)
        if (!shouldSend) return

        // Фильтр по приложению
        val targetPackage = "ru.polyphone.polyphone.megafon"
        if (sbn.packageName != targetPackage) {
            Log.d("NotificationListener", "Уведомление от ${sbn.packageName} пропущено")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("NotificationListener", "Получено уведомление от Telegram: $title - $text")

        CoroutineScope(Dispatchers.IO).launch {
            processAndSend(sbn.packageName, title, text)
        }
    }


    private suspend fun processAndSend(pkg: String, title: String, text: String) {
        val db = AppDatabase.getDatabase(this)
        val log = SmsLogEntity(
            sender = title.ifEmpty { pkg },
            sms_text = text,
            timestamp = System.currentTimeMillis()
        )

        withContext(Dispatchers.IO) {
            val existingLog = db.smsLogDao().getLogBySenderAndText(log.sender, text)
            if (existingLog == null) {
                db.smsLogDao().insertLog(log)
                Log.d("NotificationListener", "Лог сохранён: $text")
            } else {
                Log.d("NotificationListener", "Дубликат уведомления, не отправляем")
                return@withContext
            }

            val deviceToken = AuthRepository(this@LogNotificationService).getDeviceToken()
            if (!deviceToken.isNullOrEmpty()) {
                sendToServer(text, deviceToken)
            } else {
                Log.e("NotificationListener", "device_token не найден")
            }
        }
    }

    private suspend fun sendToServer(text: String, token: String) {
        try {
            val smsData = SmsData(sms_text = text, api_key = token)
            val response = RetrofitInstance.apiService.sendSms(smsData)
            val json = Gson().toJson(smsData)
            Log.d("NotificationListener", "Отправка: $json")

            if (response.isSuccessful) {
                Log.d("NotificationListener", "Уведомление успешно отправлено")
            } else {
                Log.e("NotificationListener", "Ошибка при отправке: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Ошибка: ${e.message}")
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotifLog", "Уведомление удалено: ${sbn.packageName}")
    }

}
