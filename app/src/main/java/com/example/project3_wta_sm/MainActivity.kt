package com.example.project3_wta_sm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

// Main activity for user authentication
class MainActivity : AppCompatActivity() {

    // UI elements for username and password input
    private var usernameInput: EditText? = null
    private var passwordInput: EditText? = null
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connects UI elements to variables
        usernameInput = findViewById(R.id.editTextText)
        passwordInput = findViewById(R.id.editTextText2)
        val loginButton = findViewById<Button>(R.id.button)
        val registerButton = findViewById<Button>(R.id.button2)

        databaseHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance() // Firebase authentication

        checkUserSession()

        // Login button click with user authentication
        loginButton.setOnClickListener {
            val username = usernameInput?.text.toString().trim()
            val password = passwordInput?.text.toString().trim()

            println("DEBUG: Login button clicked - Username: $username")

            // Validate input fields
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both your username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Username follows email format for Firebase authentication
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Login in background to prevent issues with skipped frames
            CoroutineScope(Dispatchers.IO).launch {
                val loginSuccess = databaseHelper.checkLogin(username, password)

                withContext(Dispatchers.Main) {
                    if (loginSuccess) {
                        Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                        navigateToDataGrid(username)
                    } else {
                        // Firebase authentication if local login fails
                        auth.signInWithEmailAndPassword(username, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val firebaseUser = auth.currentUser
                                    val firebaseUsername = firebaseUser?.email ?: "Unknown User"
                                    Toast.makeText(this@MainActivity, "Firebase Login successful", Toast.LENGTH_SHORT).show()
                                    navigateToDataGrid(firebaseUsername)
                                } else {
                                    Toast.makeText(this@MainActivity, "Incorrect login information.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }
        }

        // Register button click for new user registration
        registerButton.setOnClickListener {
            val username = usernameInput?.text.toString().trim()
            val password = passwordInput?.text.toString().trim()

            println("DEBUG: Register button clicked - Username: $username")

            // Validate input fields
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both your username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ensure username email format for Firebase authentication
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Registration in SQLite database
            CoroutineScope(Dispatchers.IO).launch {
                val success = databaseHelper.insertLogin(this@MainActivity, username, password)

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@MainActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                        usernameInput?.setText("")
                        passwordInput?.setText("")
                    }

                    // Register in Firebase as well
                    auth.createUserWithEmailAndPassword(username, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@MainActivity, "Firebase Registration successful", Toast.LENGTH_SHORT).show()
                                usernameInput?.setText("")
                                passwordInput?.setText("")
                            } else {
                                Toast.makeText(this@MainActivity, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    // New user session when app is opened
    private fun checkUserSession() {
        auth.signOut()
    }

    // Navigates to DataGridActivity after successful login
    private fun navigateToDataGrid(username: String) {
        val intent = Intent(this, DataGridActivity::class.java)
        intent.putExtra("USERNAME", username)
        startActivity(intent)
    }

    // Closes database connection
    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
    }
}