package com.example.project3_wta_sm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DataGridActivity : AppCompatActivity() {

    // Database helper
    private lateinit var databaseHelper: DatabaseHelper

    // Interface elements
    private lateinit var goalWeightInput: EditText
    private lateinit var currentWeightInput: EditText
    private lateinit var currentGoalWeightText: TextView
    private lateinit var todaysWeightText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_grid)

        // Initialize the DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Initialize the interface elements
        goalWeightInput = findViewById(R.id.goalWeightInput)
        currentWeightInput = findViewById(R.id.currentWeightInput)
        currentGoalWeightText = findViewById(R.id.currentGoalWeightText)
        todaysWeightText = findViewById(R.id.todaysWeightText)
        recyclerView = findViewById(R.id.dataGridRecyclerView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load daily weights to RecyclerView
        loadDailyWeights()

        currentGoalWeightText.text = getString(
            R.string.current_goal_weight_format,
            databaseHelper.goalWeight
        )
        todaysWeightText.text = getString(
            R.string.current_weight_value_format,
            databaseHelper.latestWeight
        )

        // Set new goal weight
        val setGoalButton = findViewById<Button>(R.id.setGoalButton)
        setGoalButton.setOnClickListener {
            val goalWeightText = goalWeightInput.text.toString()
            if (goalWeightText.isNotEmpty()) {
                val goalWeight = goalWeightText.toDouble()
                databaseHelper.insertGoalWeight(goalWeight)
                currentGoalWeightText.text = getString(
                    R.string.current_goal_weight_format,
                    goalWeight
                )
            }
        }

        // Set new daily weight
        val addDailyWeightButton = findViewById<Button>(R.id.addDailyWeightButton)
        addDailyWeightButton.setOnClickListener {
            val dailyWeightText = currentWeightInput.text.toString()
            if (dailyWeightText.isNotEmpty()) {
                val dailyWeight = dailyWeightText.toDouble()
                val date = currentDate
                databaseHelper.insertDailyWeight(date, dailyWeight)
                todaysWeightText.text = getString(
                    R.string.current_weight_value_format,
                    dailyWeight
                )
                loadDailyWeights()

                // Check if user has reached goal weight
                val goalWeight = databaseHelper.goalWeight
                if (dailyWeight <= goalWeight) {
                    Toast.makeText(
                        this,
                        "Congratulations! You've reached your goal weight!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Navigate to SMS permission activity
        val smsPermissionButton = findViewById<Button>(R.id.smsPermissionButton)
        smsPermissionButton.setOnClickListener {
            val intent = Intent(this, SMSPermissionActivity::class.java)
            startActivity(intent)
        }
    }

    // Load daily weights and update RecyclerView
    private fun loadDailyWeights() {
        val dailyWeights = databaseHelper.allDailyWeights
        val dataGridAdapter = DataGridAdapter(dailyWeights, object : DataGridAdapter.OnDeleteClickListener {
            override fun onDeleteClick(date: String, weight: Double) {
                databaseHelper.deleteWeightEntry(date, weight)
                loadDailyWeights()
            }
        })
        recyclerView.adapter = dataGridAdapter
    }

    // Sets current date to format yyyy-MM-dd
    private val currentDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
}