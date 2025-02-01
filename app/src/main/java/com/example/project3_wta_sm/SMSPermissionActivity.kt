package com.example.project3_wta_sm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SMSPermissionActivity : AppCompatActivity() {

    // Launcher for requests
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_permission)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // SMS notifications enabled
                Toast.makeText(
                    this,
                    "SMS notifications have been enabled!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // SMS permission denied
                Toast.makeText(
                    this,
                    "SMS notifications permission has been denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Listener
        val enableNotificationsButton = findViewById<Button>(R.id.enableNotificationsButton)
        enableNotificationsButton.setOnClickListener {
            askForSmsPermission()
        }
    }

    // Requests permission
    private fun askForSmsPermission() {
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Notify user that notifications are enabled
            Toast.makeText(this, "SMS notifications have already been enabled!", Toast.LENGTH_SHORT).show()
        } else {
            // Notify user that permission is  requested
            Toast.makeText(this, "Requesting SMS permission...", Toast.LENGTH_SHORT).show()
            // Launch the permission request
            requestPermissionLauncher?.launch(Manifest.permission.SEND_SMS)
        }
    }
}