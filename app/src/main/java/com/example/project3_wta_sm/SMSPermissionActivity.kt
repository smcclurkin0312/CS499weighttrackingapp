package com.example.project3_wta_sm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SMSPermissionActivity : AppCompatActivity() {

    // Launcher for requesting SMS permission
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    // SharedPreferences for storing SMS permission status
    private val prefs by lazy { getSharedPreferences("AppPrefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_permission)

        // Initialize permission request
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                prefs.edit().putBoolean("SMS_ENABLED", true).apply()

                Toast.makeText(
                    this,
                    "SMS notifications have been enabled!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "SMS notifications permission has been denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Listener for enabling SMS notifications
        val enableNotificationsButton = findViewById<Button>(R.id.enableNotificationsButton)
        enableNotificationsButton.setOnClickListener {
            askForSmsPermission()
        }
    }

    // Requests SMS permission
    private fun askForSmsPermission() {
        val isSmsEnabled = prefs.getBoolean("SMS_ENABLED", false)

        if (isSmsEnabled) {
            // Notify user SMS notifications are already enabled
            Toast.makeText(this, "SMS notifications have already been enabled!", Toast.LENGTH_SHORT).show()
        } else {
            // Request permission if not already granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Save SMS permission status
                prefs.edit().putBoolean("SMS_ENABLED", true).apply()
                Toast.makeText(this, "SMS notifications have been enabled!", Toast.LENGTH_SHORT).show()
            } else {
                // Request SMS permission from user
                Toast.makeText(this, "Requesting SMS permission...", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher?.launch(Manifest.permission.SEND_SMS)
            }
        }
    }
}