package com.example.sms
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.sms.database.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository(this)
        tokenInput = findViewById(R.id.tokenInput)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val token = tokenInput.text.toString()
            if (token.isNotEmpty()) {
                lifecycleScope.launch {
                    if (authRepository.isTokenValid(token)) {
                        authRepository.saveToken(token)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Неверный токен", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
