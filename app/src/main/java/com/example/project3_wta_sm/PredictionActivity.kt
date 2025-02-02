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
import java.text.SimpleDateFormat
import java.util.*

class PredictionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        // Initialize UI
        val timeFrameSpinner = findViewById<Spinner>(R.id.timeFrameSpinner)
        val calculateButton = findViewById<Button>(R.id.calculatePredictionButton)
        val predictionResult = findViewById<TextView>(R.id.predictionResultText)
        val weightTrendChart = findViewById<LineChart>(R.id.weightTrendChart)

        // Load time frame options
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.time_frame_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeFrameSpinner.adapter = adapter

        // Retrieve previous weight data
        val databaseHelper = DatabaseHelper(this)
        val weightEntries = databaseHelper.allDailyWeights

        if (weightEntries.size >= 2) {
            generateWeightTrendChart(weightTrendChart, weightEntries) // Generate graph
        } else {
            weightTrendChart.clear()
        }

        // Button click to calculate weight prediction for week(s)
        calculateButton.setOnClickListener {
            val selectedWeeks = timeFrameSpinner.selectedItem.toString().split(" ")[0].toInt()

            // Check for data before making a prediction
            if (weightEntries.size < 2) {
                predictionResult.text = getString(R.string.insufficient_data_message)
                return@setOnClickListener
            }

            // Calculate average weekly weight change
            val firstEntry = weightEntries.first()
            val lastEntry = weightEntries.last()
            val weightChangePerWeek = ((lastEntry.weight - firstEntry.weight) / weightEntries.size).toFloat()

            // Predict future weight based on time
            val predictedWeight = lastEntry.weight + (weightChangePerWeek * selectedWeeks)

            // Display predicted weight for week
            predictionResult.text = getString(R.string.prediction_result, selectedWeeks, predictedWeight)
        }
    }

    private fun generateWeightTrendChart(chart: LineChart, pastWeights: List<DataGridItem>) {
        val recordedEntries = mutableListOf<Entry>() // Stores recorded weight data points
        val predictedEntries = mutableListOf<Entry>() // Stores predicted weight data points
        val labels = mutableListOf<String>() // Stores x-axis labels

        // Use last 4 weeks of recorded data or available user data
        val filteredWeights = pastWeights.takeLast(28) // 4 weeks (7 days * 4)

        // Dates on x-axis
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        // Weight data points and labels at 4-day intervals
        for ((index, data) in filteredWeights.withIndex()) {
            recordedEntries.add(Entry(index.toFloat(), data.weight.toFloat()))
            if (index % 4 == 0) {
                labels.add(dateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(data.date)!!))
            } else {
                labels.add("")
            }
        }

        // Retrieve last recorded weight
        val lastRecordedWeight = filteredWeights.lastOrNull()?.weight?.toFloat() ?: return
        val lastIndex = recordedEntries.size.toFloat()

        // Last recorded weight into first predicted point
        predictedEntries.add(Entry(lastIndex, lastRecordedWeight))
        labels.add("Start Prediction")

        // Predicted weight points for up to 4 weeks
        for (week in 1..4) {
            val futureIndex = (lastIndex + (week * 7)).toFloat()
            val projectedWeight = lastRecordedWeight - (week * (lastRecordedWeight - 170f) / 4f)

            predictedEntries.add(Entry(futureIndex, projectedWeight))
            labels.add("Week $week")
        }

        // Create recorded weight trend (blue line)
        val recordedDataSet = LineDataSet(recordedEntries, "Recorded Weight")
        recordedDataSet.color = Color.BLUE
        recordedDataSet.valueTextColor = Color.BLACK
        recordedDataSet.setCircleColor(Color.BLUE)
        recordedDataSet.lineWidth = 2f
        recordedDataSet.circleRadius = 4f
        recordedDataSet.setDrawValues(false)

        // Create predicted weight trend (red dashed line)
        val predictedDataSet = LineDataSet(predictedEntries, "Predicted Weight")
        predictedDataSet.color = Color.RED
        predictedDataSet.valueTextColor = Color.BLACK
        predictedDataSet.setCircleColor(Color.RED)
        predictedDataSet.lineWidth = 2f
        predictedDataSet.circleRadius = 4f
        predictedDataSet.enableDashedLine(10f, 5f, 0f)
        predictedDataSet.setDrawValues(false)

        val lineData = LineData(recordedDataSet, predictedDataSet)
        chart.data = lineData

        // Chart appearance
        chart.description = Description().apply { text = "Weight Trend (Recorded & Predicted)" }
        chart.setBackgroundColor(Color.WHITE)
        chart.setDrawGridBackground(false)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.setDrawGridLines(false)

        // X-axis labels
        val xAxis = chart.xAxis
        xAxis.setLabelCount(labels.size / 4, true) // Display labels every 4 days
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 5f
        xAxis.isGranularityEnabled = true

        chart.invalidate() // Refresh chart
    }
}