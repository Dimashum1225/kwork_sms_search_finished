package com.example.sms

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.sms.Listeners.SmsListenerService
import com.example.sms.database.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ComponentName
import android.provider.Settings
import com.example.sms.core.NotificationHelper
import com.example.sms.core.PermissionManager

class MainActivity : AppCompatActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var statusText: TextView
    private lateinit var stopServiceButton: Button
    private lateinit var startServiceButton: Button
    private lateinit var logsButton: Button
    private lateinit var changeTokenButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        stopServiceButton = findViewById(R.id.stopServiceButton)
        startServiceButton = findViewById(R.id.startServiceButton) // Кнопка включить сервис
        logsButton = findViewById(R.id.logsButton)
        changeTokenButton = findViewById(R.id.changeTokenButton)

        NotificationHelper.ensureListenerEnabled(this)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "test",
                "Test Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }



        permissionManager = PermissionManager(this)
        permissionManager.init { contract, callback ->
            registerForActivityResult(contract, callback)
        }

        permissionManager.requestSequential(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        ) {
            updatePermissionsUI()
        }

        requestIgnoreBatteryOptimizations()
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))


        // Инициализация репозитория для работы с токеном
        authRepository = AuthRepository(this)

        // Проверяем, запущен ли сервис
        if (isServiceRunning(SmsListenerService::class.java)) {
            statusText.text = "Сервис активен"
            startServiceButton.isEnabled = false // Делаем кнопку неактивной, если сервис уже работает
        } else {
            statusText.text = "Сервис не активен"
            startServiceButton.isEnabled = true // Делаем кнопку активной, если сервис не работает
        }


        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)



        startServiceButton.setOnClickListener {
            sharedPreferences.edit()
                .putBoolean("manual_stop", false)
                .putBoolean("shouldSendNotification", true)
                .apply()

            val flag = sharedPreferences.getBoolean("shouldSendNotification", false)
            Log.d("PrefsCheck", "После старта: shouldSendNotification = $flag")

            startSmsListenerService()
            statusText.text = "Сервис запущен"
        }
        stopServiceButton.setOnClickListener {
            sharedPreferences.edit()
                .putBoolean("manual_stop", true)
                .putBoolean("shouldSendNotification", false) // Выключаем отправку уведомлений
                .apply()

            stopService(Intent(this, SmsListenerService::class.java))
            statusText.text = "Сервис остановлен вручную"
        }



        logsButton.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }

        changeTokenButton.setOnClickListener {
            lifecycleScope.launch {
                authRepository.deleteToken()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun updatePermissionsUI() {
        val status = StringBuilder("Статус разрешений:\n")

        if (permissionManager.isGranted(Manifest.permission.RECEIVE_SMS)) {
            status.append("✅ Разрешение на SMS выдано\n")
        } else {
            status.append("⛔ Нет разрешения на SMS\n")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissionManager.isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                status.append("✅ Разрешение на уведомления выдано\n")
            } else {
                status.append("⛔ Нет разрешения на уведомления\n")
            }
        }

        findViewById<TextView>(R.id.permissionsStatusText).text = status.toString()
    }




    private fun startSmsListenerService() {
        val serviceIntent = Intent(this, SmsListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent) // Для Android 8.0+
        } else {
            startService(serviceIntent) // Для старых версий
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {

        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // Если разрешение не предоставлено, открываем настройки для запроса
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                // Разрешение уже дано, можно продолжить
                Log.d("BatteryOptimization", "Разрешение на оптимизацию батареи уже предоставлено.")
            }
        }
    }



    override fun onResume() {
        super.onResume()
        updatePermissionsUI()
    }

}

