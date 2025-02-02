package com.example.project3_wta_sm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// User login and registration functionality
class MainActivity : AppCompatActivity() {

    // Username and password input
    private var usernameInput: EditText? = null
    private var passwordInput: EditText? = null
    private var databaseHelper: DatabaseHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect UI XML to Kotlin variables
        usernameInput = findViewById(R.id.editTextText)
        passwordInput = findViewById(R.id.editTextText2)
        val loginButton = findViewById<Button>(R.id.button)
        val registerButton = findViewById<Button>(R.id.button2)

        databaseHelper = DatabaseHelper(this)

        // Listener for login button
        loginButton.setOnClickListener { _: View? ->
            val username = usernameInput?.text.toString().trim()
            val password = passwordInput?.text.toString().trim()

            // Check for empty fields
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both your username and your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check login info
            if (databaseHelper?.checkLogin(username, password) == true) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DataGridActivity::class.java)
                startActivity(intent) // Successful login activity
            } else {
                // Invalid login
                Toast.makeText(this, "Incorrect login information.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener for register button
        registerButton.setOnClickListener { _: View? ->
            val username = usernameInput?.text.toString().trim()
            val password = passwordInput?.text.toString().trim()

            // Check for empty field
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both your username and your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Insert new login info to database
            if (databaseHelper?.insertLogin(username, password) == true) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                usernameInput?.setText("")
                passwordInput?.setText("")
            } else {
                // Invalid registration
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Close database connection
    override fun onDestroy() {
        super.onDestroy()
        databaseHelper?.close()
    }
}