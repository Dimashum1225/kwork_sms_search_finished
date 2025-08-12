package com.example.sms.core

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {
    private val permissionQueue: MutableList<String> = mutableListOf()
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var currentCallback: ((Boolean) -> Unit)? = null

    fun init(register: (ActivityResultContracts.RequestPermission, (Boolean) -> Unit) -> ActivityResultLauncher<String>) {
        permissionLauncher = register(ActivityResultContracts.RequestPermission()) { isGranted ->
            currentCallback?.invoke(isGranted)
            permissionQueue.removeFirstOrNull()
            launchNext()
        }
    }

    fun requestSequential(vararg permissions: String, onComplete: () -> Unit) {
        permissionQueue.clear()
        permissionQueue.addAll(permissions.filter {
            !isGranted(it)
        })

        if (permissionQueue.isEmpty()) {
            onComplete()
        } else {
            this.currentCallback = {
                if (permissionQueue.isEmpty()) onComplete()
            }
            launchNext()
        }
    }

    private fun launchNext() {
        if (permissionQueue.isNotEmpty()) {
            val nextPermission = permissionQueue.first()
            permissionLauncher.launch(nextPermission)
        }
    }

    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
