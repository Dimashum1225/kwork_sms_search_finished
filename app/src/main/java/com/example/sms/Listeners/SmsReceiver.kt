package com.example.sms.Listeners

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.sms.database.AppDatabase
import com.example.sms.database.AuthRepository
import com.example.sms.database.RetrofitInstance
import com.example.sms.database.SmsData
import com.example.sms.logs.SmsLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SmsReceiver : BroadcastReceiver() {

    private val channelId = "sms_notifications"
    private val saveSmsMutex = Mutex()

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action != "android.provider.Telephony.SMS_RECEIVED") return

        val fullMessage = extractFullMessage(intent) ?: return
        val sender = fullMessage.first
        val messageText = fullMessage.second

        CoroutineScope(Dispatchers.IO).launch {
            if (processAndSaveSms(context, sender, messageText)) {
                val deviceToken = getDeviceTokenFromDb(context)
                if (!deviceToken.isNullOrEmpty()) {
                    sendSmsToServer(sender, messageText, deviceToken)
                } else {
                    Toast.makeText(context, "device_token не найден", Toast.LENGTH_SHORT).show()

                }
                withContext(Dispatchers.Main) {
                    showNotification(context, sender, messageText)
                }
            }
        }
    }



    /** Извлекает SMS-сообщение из Intent */
    private fun extractFullMessage(intent: Intent): Pair<String, String>? {
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return null
        val smsMessages = pdus.mapNotNull { pdu ->
            SmsMessage.createFromPdu(pdu as ByteArray)
        }.distinctBy { it.messageBody } // Убираем дубликаты частей

        if (smsMessages.isEmpty()) return null

        val fullMessage = smsMessages.joinToString("") { it.messageBody }
        val sender = smsMessages.firstOrNull()?.originatingAddress ?: return null

        return sender to fullMessage
    }

    /** Проверяет, есть ли сообщение в БД, и если нет — сохраняет */
    private suspend fun processAndSaveSms(context: Context, sender: String, text: String): Boolean {
        val db = AppDatabase.getDatabase(context)
        val log = SmsLogEntity(sender = sender, text = text, timestamp = System.currentTimeMillis())

        return saveSmsMutex.withLock {
            withContext(Dispatchers.IO) {
                val existingLog = db.smsLogDao().getLogBySenderAndText(sender, text)
                if (existingLog == null) {
                    db.smsLogDao().insertLog(log)
                    Log.d("messagesaved", "message saved - $text")
                    return@withContext true
                }
                return@withContext false
            }
        }
    }

    /** Получает токен устройства из БД */
    private suspend fun getDeviceTokenFromDb(context: Context): String? {
        val authRepository = AuthRepository(context)
        return authRepository.getDeviceToken()
    }

    /** Отправляет SMS на сервер */
    private suspend fun sendSmsToServer(sender: String, text: String, deviceToken: String) {
        withContext(Dispatchers.IO) {
            try {
                val smsData = SmsData(sender = sender, text = text, device_token = deviceToken)
                val response = RetrofitInstance.apiService.sendSms(smsData)

                if (response.isSuccessful) {
                    Log.d("SmsReceiver", "SMS успешно отправлен на сервер.")
                } else {
                    Log.e("SmsReceiver", "Ошибка при отправке SMS: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Ошибка: ${e.message}")
            }
        }
    }

    /** Показывает уведомление пользователю */
    private fun showNotification(context: Context, sender: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Входящие SMS", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о входящих SMS"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Новое SMS от $sender")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
