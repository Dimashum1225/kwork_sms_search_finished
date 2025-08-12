package com.example.sms.Listeners

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService

class SmsListenerService : LifecycleService() {

    private val channelId = "sms_listener_service"
    private val notificationId = 1
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var smsReceiver: SmsReceiver

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        createNotificationChannel()
        startForeground(notificationId, createNotification())

        val manualStop = sharedPreferences.getBoolean("manual_stop", false)
        val shouldRestart = sharedPreferences.getBoolean("should_restart", true)

        if (!manualStop || shouldRestart) {
            smsReceiver = SmsReceiver()
            registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

            val editor = sharedPreferences.edit()
            editor.putBoolean("manual_stop", false)
            editor.apply()
        }

        updateNotification(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SMS Listener Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Сервис активен")
            .setContentText("Прослушивание входящих сообщений")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun updateNotification(isListening: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationText = if (isListening) {
            "Приложение слушает входящие SMS"
        } else {
            "Слушание SMS остановлено"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SMS Listener")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        notificationManager.notify(notificationId, notification)
    }


    override fun onDestroy() {
        super.onDestroy()

        try {
            unregisterReceiver(smsReceiver)
        } catch (e: Exception) {
            Log.w("SmsListenerService", "Receiver already unregistered or not initialized: ${e.message}")
        }

        val isManualStop = sharedPreferences.getBoolean("manual_stop", false)
        if (isManualStop) {
            Log.d("SmsListenerService", "Сервис остановлен вручную, не перезапускаем.")
        } else {
            Log.d("SmsListenerService", "Сервис остановлен, перезапускаем...")

            sharedPreferences.edit().putBoolean("should_restart", true).apply()

            val restartIntent = Intent(applicationContext, SmsListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        }
    }

}
