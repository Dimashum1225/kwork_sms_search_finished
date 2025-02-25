package com.example.sms.Listeners

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем, если устройство перезагрузилось
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_REBOOT) {
            // Проверим, был ли сервис остановлен вручную. Если не был остановлен, запустим сервис.
            val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val manualStop = sharedPreferences.getBoolean("manual_stop", false)

            if (!manualStop) {
                // Сервис не был остановлен вручную, перезапускаем его
                val serviceIntent = Intent(context, SmsListenerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}

