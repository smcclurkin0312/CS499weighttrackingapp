package com.example.project3_wta_sm

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DataGridActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    // UI elements
    private lateinit var goalWeightInput: EditText
    private lateinit var currentWeightInput: EditText
    private lateinit var currentGoalWeightText: TextView
    private lateinit var todaysWeightText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var selectedDateText: TextView
    private lateinit var selectDateButton: Button

    private var selectedDate: String = getCurrentDate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_grid)

        databaseHelper = DatabaseHelper(this)

        // UI elements to XML components
        goalWeightInput = findViewById(R.id.goalWeightInput)
        currentWeightInput = findViewById(R.id.currentWeightInput)
        currentGoalWeightText = findViewById(R.id.currentGoalWeightText)
        todaysWeightText = findViewById(R.id.todaysWeightText)
        recyclerView = findViewById(R.id.dataGridRecyclerView)
        selectedDateText = findViewById(R.id.selectedDateText)
        selectDateButton = findViewById(R.id.selectDateButton)

        // RecyclerView to display weight history
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadDailyWeights()

        // Display most recent goal weight and weight entry
        currentGoalWeightText.text = getString(R.string.current_goal_weight_format, databaseHelper.goalWeight)
        todaysWeightText.text = getString(R.string.current_weight_value_format, databaseHelper.latestWeight)

        // Handle date selection
        selectDateButton.setOnClickListener {
            showDatePicker()
        }

        // Handle setting new goal weight
        val setGoalButton = findViewById<Button>(R.id.setGoalButton)
        setGoalButton.setOnClickListener {
            val goalWeightText = goalWeightInput.text.toString()
            if (goalWeightText.isNotEmpty()) {
                val goalWeight = goalWeightText.toDouble()
                databaseHelper.insertGoalWeight(goalWeight)
                currentGoalWeightText.text = getString(R.string.current_goal_weight_format, goalWeight)
            }
        }

        // Handle adding new daily weight entry
        val addDailyWeightButton = findViewById<Button>(R.id.addDailyWeightButton)
        addDailyWeightButton.setOnClickListener {
            val dailyWeightText = currentWeightInput.text.toString()
            if (dailyWeightText.isNotEmpty()) {
                val dailyWeight = dailyWeightText.toDouble()
                databaseHelper.insertDailyWeightManual(selectedDate, dailyWeight)
                loadDailyWeights()
            }
        }

        // Navigate to SMS permission settings
        val smsPermissionButton = findViewById<Button>(R.id.smsPermissionButton)
        smsPermissionButton.setOnClickListener {
            val intent = Intent(this, SMSPermissionActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Prediction Activity
        val predictionButton = findViewById<Button>(R.id.predictionButton)
        predictionButton.setOnClickListener {
            val intent = Intent(this, PredictionActivity::class.java)
            startActivity(intent)
        }
    }

    // Opens DatePickerDialog for manual date selection
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            selectedDate = formattedDate
            selectedDateText.text = getString(R.string.selected_date_label, formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Retrieves current date and formats it
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Loads weight entries into the RecyclerView
    private fun loadDailyWeights() {
        val dailyWeights = databaseHelper.allDailyWeights
        val adapter = DataGridAdapter(dailyWeights, object : DataGridAdapter.OnDeleteClickListener {
            override fun onDeleteClick(date: String, weight: Double) {
                databaseHelper.deleteWeightEntry(date, weight)
                loadDailyWeights()
            }
        })
        recyclerView.adapter = adapter
    }
}