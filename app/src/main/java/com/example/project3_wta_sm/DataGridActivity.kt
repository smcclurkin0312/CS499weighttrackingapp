package com.example.project3_wta_sm

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper

class DataGridActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null

    // UI components
    private lateinit var goalWeightInput: EditText
    private lateinit var currentWeightInput: EditText
    private lateinit var currentGoalWeightText: TextView
    private lateinit var todaysWeightText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var selectedDateText: TextView
    private lateinit var selectDateButton: Button
    private lateinit var addDailyWeightButton: Button
    private lateinit var smsPermissionButton: Button
    private lateinit var predictionButton: Button
    private lateinit var exportDataButton: Button
    private lateinit var importDataButton: Button
    private lateinit var adapter: DataGridAdapter

    private var selectedDate: String = getCurrentDate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_grid)

        // Database helper and authentication
        databaseHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.email

        // Ensure user is logged in, otherwise close the activity
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No user is logged in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize UI components
        goalWeightInput = findViewById(R.id.goalWeightInput)
        currentWeightInput = findViewById(R.id.currentWeightInput)
        currentGoalWeightText = findViewById(R.id.currentGoalWeightText)
        todaysWeightText = findViewById(R.id.todaysWeightText)
        recyclerView = findViewById(R.id.dataGridRecyclerView)
        selectedDateText = findViewById(R.id.selectedDateText)
        selectDateButton = findViewById(R.id.selectDateButton)
        addDailyWeightButton = findViewById(R.id.addDailyWeightButton)
        smsPermissionButton = findViewById(R.id.smsPermissionButton)
        predictionButton = findViewById(R.id.predictionButton)
        exportDataButton = findViewById(R.id.exportDataButton)
        importDataButton = findViewById(R.id.importDataButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadDailyWeights()
        updateWeightDisplay()

        // Set goal weight button
        findViewById<Button>(R.id.setGoalButton).setOnClickListener {
            val goalWeightText = goalWeightInput.text.toString()
            if (goalWeightText.isNotEmpty()) {
                val goalWeight = goalWeightText.toDouble()
                databaseHelper.insertGoalWeight(userId!!, goalWeight)
                updateWeightDisplay()
                goalWeightInput.text.clear()
                Toast.makeText(this, "Goal weight has been set!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a goal weight.", Toast.LENGTH_SHORT).show()
            }
        }

        // Select date button
        selectDateButton.setOnClickListener { showDatePicker() }

        // Add daily weight button
        addDailyWeightButton.setOnClickListener {
            val dailyWeightText = currentWeightInput.text.toString()
            if (dailyWeightText.isNotEmpty()) {
                val dailyWeight = dailyWeightText.toDouble()

                // Clear test data
                if (databaseHelper.hasTestData(userId!!)) {
                    databaseHelper.clearTestData(userId!!)
                }

                databaseHelper.insertDailyWeightManual(userId!!, selectedDate, dailyWeight)
                loadDailyWeights()
                updateWeightDisplay()
                currentWeightInput.text.clear()
                Toast.makeText(this, "Weight entry has been added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a weight.", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to SMS Permission activity
        smsPermissionButton.setOnClickListener {
            startActivity(Intent(this, SMSPermissionActivity::class.java))
        }

        // Navigate to Weight Prediction activity
        predictionButton.setOnClickListener {
            startActivity(Intent(this, PredictionActivity::class.java))
        }

        // Firestore Export Button
        exportDataButton.setOnClickListener {
            databaseHelper.exportDataToFirestore(this)
        }

        // Firestore Import Button
        importDataButton.setOnClickListener {
            databaseHelper.importDataFromFirestore(this)

            // Delay UI refresh for database
            Handler(Looper.getMainLooper()).postDelayed({
                loadDailyWeights()
                updateWeightDisplay()
            }, 500)
        }
    }

    // Displays date picker dialog for custom date
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            selectedDateText.text = getString(R.string.selected_date_label, selectedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Gets current date in yyyy-MM-dd format
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Loads daily weight entries into the RecyclerView
    private fun loadDailyWeights() {
        if (userId == null) return
        val dailyWeights = databaseHelper.getAllDailyWeights(userId!!).toMutableList()
        adapter = DataGridAdapter(dailyWeights, object : DataGridAdapter.OnDeleteClickListener {
            override fun onDeleteClick(date: String, weight: Double) {
                val position = dailyWeights.indexOfFirst { it.date == date && it.weight == weight }
                if (position != -1) {
                    databaseHelper.deleteWeightEntry(userId!!, date, weight)
                    dailyWeights.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateWeightDisplay()
                }
            }
        })
        recyclerView.adapter = adapter
    }

    // Updatess UI to display latest goal weight and today's weight
    private fun updateWeightDisplay() {
        if (userId == null) return
        currentGoalWeightText.text =
            getString(R.string.current_goal_weight_format, databaseHelper.getGoalWeight(userId!!))
        todaysWeightText.text =
            getString(R.string.current_weight_value_format, databaseHelper.getLatestWeight(userId!!))
    }
}