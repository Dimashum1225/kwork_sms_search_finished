package com.example.sms.core

import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceController {

    fun start(context: Context, serviceClass: Class<*>) {
        val intent = Intent(context, serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context, serviceClass: Class<*>) {
        val intent = Intent(context, serviceClass)
        context.stopService(intent)
    }
}
