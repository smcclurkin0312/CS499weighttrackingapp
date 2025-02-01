package com.example.project3_wta_sm

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// DatabaseHelper class manages SQLite database
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create DailyWeight table
        val createDailyWeightTable = """
            CREATE TABLE $TABLE_DAILY_WEIGHT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT,
                $COLUMN_WEIGHT REAL
            )
        """.trimIndent()
        db.execSQL(createDailyWeightTable)

        // Create GoalWeight table
        val createGoalWeightTable = """
            CREATE TABLE $TABLE_GOAL_WEIGHT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GOAL_WEIGHT REAL
            )
        """.trimIndent()
        db.execSQL(createGoalWeightTable)

        // Create Login table
        val createLoginTable = """
            CREATE TABLE $TABLE_LOGIN (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT,
                $COLUMN_PASSWORD TEXT
            )
        """.trimIndent()
        db.execSQL(createLoginTable)
    }

    // Removes current/old tables and recreates updated tables when new data is provided
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GOAL_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGIN")
        onCreate(db)
    }

    // Inserts the daily weight entry into DailyWeight table
    fun insertDailyWeight(date: String, weight: Double) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_DATE, date)
                put(COLUMN_WEIGHT, weight)
            }
            db.insert(TABLE_DAILY_WEIGHT, null, values)
        }
    }

    // Inserts the goal weight entry into GoalWeight table
    fun insertGoalWeight(goalWeight: Double) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_GOAL_WEIGHT, goalWeight)
            }
            db.insert(TABLE_GOAL_WEIGHT, null, values)
        }
    }

    // Retrieves most recent goal from GoalWeight table
    val goalWeight: Double
        get() = readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT $COLUMN_GOAL_WEIGHT FROM $TABLE_GOAL_WEIGHT ORDER BY $COLUMN_ID DESC LIMIT 1", null)
            cursor.use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
        }

    // Retrieves most recent weight from DailyWeight table
    val latestWeight: Double
        get() = readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT $COLUMN_WEIGHT FROM $TABLE_DAILY_WEIGHT ORDER BY $COLUMN_ID DESC LIMIT 1", null)
            cursor.use {
                if (it.moveToFirst()) it.getDouble(0) else 0.0
            }
        }

    // Deletes weight entry by matching date and weight
    fun deleteWeightEntry(date: String, weight: Double) {
        writableDatabase.use { db ->
            db.delete(
                TABLE_DAILY_WEIGHT,
                "$COLUMN_DATE = ? AND $COLUMN_WEIGHT = ?",
                arrayOf(date, weight.toString())
            )
        }
    }

    // Retrieves daily weight entries as list using DataGridItem
    val allDailyWeights: List<DataGridItem>
        get() = readableDatabase.use { db ->
            val dailyWeights = mutableListOf<DataGridItem>()
            val cursor = db.rawQuery("SELECT * FROM $TABLE_DAILY_WEIGHT", null)
            cursor.use {
                while (it.moveToNext()) {
                    val date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE))
                    val weight = it.getDouble(it.getColumnIndexOrThrow(COLUMN_WEIGHT))
                    dailyWeights.add(DataGridItem(date, weight))
                }
            }
            dailyWeights
        }

    // Inserts new login info into Login table
    fun insertLogin(username: String, password: String): Boolean {
        return writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, username)
                put(COLUMN_PASSWORD, password)
            }
            db.insert(TABLE_LOGIN, null, values) != -1L
        }
    }

    // Checks login info against Login table
    fun checkLogin(username: String, password: String): Boolean {
        return readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_LOGIN WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(username, password)
            )
            cursor.use { it.count > 0 }
        }
    }

    companion object {
        // Database name and version
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_DAILY_WEIGHT = "DailyWeight"
        const val TABLE_GOAL_WEIGHT = "GoalWeight"
        const val TABLE_LOGIN = "Login"

        // Column names
        const val COLUMN_ID = "id"
        const val COLUMN_DATE = "date"
        const val COLUMN_WEIGHT = "weight"
        const val COLUMN_GOAL_WEIGHT = "goalWeight"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
    }
}