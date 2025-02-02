package com.example.project3_wta_sm

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create DailyWeight table to store user's weight entries
        db.execSQL("""
            CREATE TABLE $TABLE_DAILY_WEIGHT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT,
                $COLUMN_WEIGHT REAL
            )
        """.trimIndent())

        // Create GoalWeight table to store user's goal weight
        db.execSQL("""
            CREATE TABLE $TABLE_GOAL_WEIGHT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GOAL_WEIGHT REAL
            )
        """.trimIndent())

        // Create Login table to store user credentials
        db.execSQL("""
            CREATE TABLE $TABLE_LOGIN (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT
            )
        """.trimIndent())

        // Sample weight data for testing purposes
        insertSampleWeightData(db)
    }

    // Drops and recreates tables
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GOAL_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGIN")
        onCreate(db)
    }

    // Inserts new goal weight entry into GoalWeight table
    fun insertGoalWeight(goalWeight: Double) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_GOAL_WEIGHT, goalWeight)
            }
            db.insert(TABLE_GOAL_WEIGHT, null, values)
        }
    }

    // Retrieves most recent goal weight
    val goalWeight: Double
        get() = readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT $COLUMN_GOAL_WEIGHT FROM $TABLE_GOAL_WEIGHT ORDER BY $COLUMN_ID DESC LIMIT 1", null)
            cursor.use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
        }

    // Retrieves most recent weight entry from DailyWeight table
    val latestWeight: Double
        get() = readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT $COLUMN_WEIGHT FROM $TABLE_DAILY_WEIGHT ORDER BY $COLUMN_DATE DESC LIMIT 1", null)
            cursor.use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
        }

    // Deletes specific weight entry based on date and weight
    fun deleteWeightEntry(date: String, weight: Double) {
        writableDatabase.use { db ->
            db.delete(
                TABLE_DAILY_WEIGHT,
                "$COLUMN_DATE = ? AND $COLUMN_WEIGHT = ?",
                arrayOf(date, weight.toString())
            )
        }
    }

    // Inserts weight entry with manually selected date
    fun insertDailyWeightManual(customDate: String, weight: Double) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_DATE, customDate)
                put(COLUMN_WEIGHT, weight)
            }
            db.insert(TABLE_DAILY_WEIGHT, null, values)
        }
    }

    // Sample weight data for testing purposes (preloaded data)
    private fun insertSampleWeightData(db: SQLiteDatabase) {
        val sampleWeights = listOf(
            Pair("2024-01-01", 180.0),
            Pair("2024-01-08", 178.5),
            Pair("2024-01-15", 177.0),
            Pair("2024-01-22", 175.8),
            Pair("2024-01-29", 174.2)
        )

        for ((date, weight) in sampleWeights) {
            val values = ContentValues().apply {
                put(COLUMN_DATE, date)
                put(COLUMN_WEIGHT, weight)
            }
            db.insert(TABLE_DAILY_WEIGHT, null, values)
        }
    }

    // Retrieves all weight entries from DailyWeight table (by date, in ascending order)
    val allDailyWeights: List<DataGridItem>
        get() = readableDatabase.use { db ->
            val dailyWeights = mutableListOf<DataGridItem>()
            val cursor = db.rawQuery("SELECT * FROM $TABLE_DAILY_WEIGHT ORDER BY $COLUMN_DATE ASC", null)
            cursor.use {
                while (it.moveToNext()) {
                    val date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE))
                    val weight = it.getDouble(it.getColumnIndexOrThrow(COLUMN_WEIGHT))
                    dailyWeights.add(DataGridItem(date, weight))
                }
            }
            dailyWeights
        }

    // Checks if username and password exist in Login table
    fun checkLogin(username: String, password: String): Boolean {
        return readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_LOGIN WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(username, password)
            )
            cursor.use { it.count > 0 }
        }
    }

    // Inserts new login entry into Login table
    fun insertLogin(username: String, password: String): Boolean {
        return writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, username)
                put(COLUMN_PASSWORD, password)
            }
            db.insert(TABLE_LOGIN, null, values) != -1L
        }
    }

    companion object {
        // Database info
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 1

        // Table names
        private const val TABLE_DAILY_WEIGHT = "DailyWeight"
        private const val TABLE_GOAL_WEIGHT = "GoalWeight"
        private const val TABLE_LOGIN = "Login"

        // Column names
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_WEIGHT = "weight"
        private const val COLUMN_GOAL_WEIGHT = "goalWeight"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
    }
}