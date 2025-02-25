package com.example.sms

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sms.database.AppDatabase
import com.example.sms.logs.SmsLogAdapter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LogsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SmsLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SmsLogAdapter(emptyList())
        recyclerView.adapter = adapter

        // Загружаем последние сообщения
        loadSmsLogs()
    }

    private fun loadSmsLogs() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Получаем последние сообщения
            val db = AppDatabase.getDatabase(applicationContext)
            val logs = db.smsLogDao().getLastLogs()

            // Обновляем UI в основном потоке
            launch(Dispatchers.Main) {
                adapter.updateLogs(logs)
            }
        }
    }
}
