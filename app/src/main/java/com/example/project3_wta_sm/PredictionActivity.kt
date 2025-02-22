package com.example.project3_wta_sm

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.data.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class PredictionActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null // Store the logged-in user ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        // Initialize database and authentication
        databaseHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.email // Email is user ID

        if (userId == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize UI elements
        val timeFrameSpinner = findViewById<Spinner>(R.id.timeFrameSpinner)
        val calculateButton = findViewById<Button>(R.id.calculatePredictionButton)
        val predictionResult = findViewById<TextView>(R.id.predictionResultText)
        val weightTrendChart = findViewById<LineChart>(R.id.weightTrendChart)

        // Load time frame options into spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.time_frame_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeFrameSpinner.adapter = adapter

        // Retrieve weight history from database
        val weightEntries = databaseHelper.getAllDailyWeights(userId!!)

        // Requires at least 2 entries
        if (weightEntries.size >= 2) {
            generateWeightTrendChart(weightTrendChart, weightEntries)
        } else {
            weightTrendChart.clear()
        }

        // Calculate prediction
        calculateButton.setOnClickListener {
            val selectedWeeks = timeFrameSpinner.selectedItem.toString().split(" ")[0].toInt()

            // Check for sufficient data
            if (weightEntries.size < 2) {
                predictionResult.text = getString(R.string.insufficient_data_message)
                return@setOnClickListener
            }

            // Compute time span between first and last entry
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val firstEntry = weightEntries.first()
            val lastEntry = weightEntries.last()

            val firstDate = dateFormat.parse(firstEntry.date) ?: return@setOnClickListener
            val lastDate = dateFormat.parse(lastEntry.date) ?: return@setOnClickListener
            val daysBetween = ((lastDate.time - firstDate.time) / (1000 * 60 * 60 * 24)).toFloat()

            // Weight calculations set to use Float
            val weightChangePerDay: Float = if (daysBetween > 0) {
                (lastEntry.weight.toFloat() - firstEntry.weight.toFloat()) / daysBetween
            } else {
                0f
            }
            val weightChangePerWeek: Float = weightChangePerDay * 7

            // Predict future weight based on actual weight trend
            val predictedWeight: Float = lastEntry.weight.toFloat() + (weightChangePerWeek * selectedWeeks)

            // Pass the float directly
            predictionResult.text = getString(R.string.prediction_result, selectedWeeks, predictedWeight)
        }
    }

    // Generates weight trend chart with only recorded weight trends
    private fun generateWeightTrendChart(chart: LineChart, pastWeights: List<DataGridItem>) {
        val recordedEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        // Get last 4 weeks of data or available user data
        val filteredWeights = pastWeights.takeLast(28)

        // Format for dates
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        // Populate recorded weight data points and X-axis labels
        for ((index, data) in filteredWeights.withIndex()) {
            recordedEntries.add(Entry(index.toFloat(), data.weight.toFloat()))

            // Label 4th data points
            if (index % 4 == 0) {
                labels.add(dateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(data.date)!!))
            } else {
                labels.add("")
            }
        }

        // Recorded Weight (Blue Line)
        val recordedDataSet = LineDataSet(recordedEntries, "Recorded Weight").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        chart.data = LineData(recordedDataSet)

        // Chart appearance
        chart.description = Description().apply { text = "Current Recorded Weight Progress Graph" }
        chart.setBackgroundColor(Color.WHITE)
        chart.setDrawGridBackground(false)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)

        // X-axis labels
        val xAxis = chart.xAxis
        xAxis.setLabelCount(labels.size / 4, true)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 5f
        xAxis.isGranularityEnabled = true

        // Refresh chart
        chart.invalidate()
    }
}