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
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.sms.Listeners.SmsListenerService
import com.example.sms.database.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var statusText: TextView
    private lateinit var stopServiceButton: Button
    private lateinit var startServiceButton: Button
    private lateinit var logsButton: Button
    private lateinit var changeTokenButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        stopServiceButton = findViewById(R.id.stopServiceButton)
        startServiceButton = findViewById(R.id.startServiceButton) // Кнопка включить сервис
        logsButton = findViewById(R.id.logsButton)
        changeTokenButton = findViewById(R.id.changeTokenButton)

        checkAndRequestPermissions() // Запрос разрешений
        requestIgnoreBatteryOptimizations()

        // Запрашиваем разрешение на уведомления, если Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

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


        stopServiceButton.setOnClickListener {
            // Сохраняем флаг, что сервис был остановлен вручную
            val editor = sharedPreferences.edit()
            editor.putBoolean("manual_stop", true) // Сервис остановлен вручную
            editor.apply()

            // Останавливаем сервис
            stopService(Intent(this, SmsListenerService::class.java))

            // Обновляем статус
            statusText.text = "Сервис остановлен вручную"
        }

        startServiceButton.setOnClickListener {
            // Сбрасываем флаг manual_stop при включении сервиса
            val editor = sharedPreferences.edit()
            editor.putBoolean("manual_stop", false) // Очищаем флаг
            editor.apply()

            // Запускаем сервис
            startService(Intent(this, SmsListenerService::class.java))

            // Обновляем статус
            statusText.text = "Сервис запущен"
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

    private fun checkAndRequestPermissions() {
        val permissionsStatus = StringBuilder("Статус разрешений:\n")

        lifecycleScope.launch {
            if (!requestPermission(Manifest.permission.RECEIVE_SMS)) {
                permissionsStatus.append("⛔ Нет разрешения на SMS\n")
            } else {
                permissionsStatus.append("✅ Разрешение на SMS выдано\n")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!requestPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                    permissionsStatus.append("⛔ Нет разрешения на уведомления\n")
                } else {
                    permissionsStatus.append("✅ Разрешение на уведомления выдано\n")
                }
            }

            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.permissionsStatusText).text = permissionsStatus.toString()
            }
        }
    }


    private suspend fun requestPermission(permission: String): Boolean {

        return withContext(Dispatchers.Main) {
            val requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (!isGranted) {
                        Toast.makeText(this@MainActivity, "Разрешение $permission не предоставлено", Toast.LENGTH_SHORT).show()
                    }
                }

            if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
            ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED
        }
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


    private fun requestNotificationPermission() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Разрешение на уведомления не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

