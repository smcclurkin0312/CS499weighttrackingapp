package com.example.project3_wta_sm

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val firestore = FirebaseFirestore.getInstance() // Firestore instance
    private val auth = FirebaseAuth.getInstance() // Firebase Auth instance

    override fun onCreate(db: SQLiteDatabase) {
        // Create table for storing user daily weights
        db.execSQL("""
            CREATE TABLE $TABLE_DAILY_WEIGHT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_WEIGHT REAL,
                UNIQUE ($COLUMN_USER_ID, $COLUMN_DATE) ON CONFLICT REPLACE,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_LOGIN($COLUMN_USERNAME) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create table for storing user goal weights
        db.execSQL("""
            CREATE TABLE $TABLE_GOAL_WEIGHT (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_GOAL_WEIGHT REAL,
                UNIQUE ($COLUMN_USER_ID) ON CONFLICT REPLACE,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_LOGIN($COLUMN_USERNAME) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create table for storing login info
        db.execSQL("""
            CREATE TABLE $TABLE_LOGIN (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GOAL_WEIGHT")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGIN")
        onCreate(db)
    }

    // Clears any test data for a user before adding real weight entries
    fun clearTestData(userId: String) {
        writableDatabase.use { db ->
            db.delete(TABLE_DAILY_WEIGHT, "$COLUMN_USER_ID = ?", arrayOf(userId))
        }
    }

    // Checks for test data
    fun hasTestData(userId: String): Boolean {
        return readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_DAILY_WEIGHT WHERE $COLUMN_USER_ID = ? AND $COLUMN_DATE < '2024-02-01'",
                arrayOf(userId)
            )
            cursor.use { it.moveToFirst() && it.getInt(0) > 0 }
        }
    }

    // Retrieves most recent goal weight for user
    fun getGoalWeight(userId: String): Double {
        return readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT $COLUMN_GOAL_WEIGHT FROM $TABLE_GOAL_WEIGHT WHERE $COLUMN_USER_ID = ? ORDER BY $COLUMN_ID DESC LIMIT 1",
                arrayOf(userId)
            )
            cursor.use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
        }
    }

    // Inserts goal weight for user
    fun insertGoalWeight(userId: String, goalWeight: Double) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
                put(COLUMN_GOAL_WEIGHT, goalWeight)
            }
            db.insertWithOnConflict(TABLE_GOAL_WEIGHT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // Retrieves most recent weight for user
    fun getLatestWeight(userId: String): Double {
        return readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT $COLUMN_WEIGHT FROM $TABLE_DAILY_WEIGHT WHERE $COLUMN_USER_ID = ? ORDER BY $COLUMN_DATE DESC LIMIT 1",
                arrayOf(userId)
            )
            cursor.use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
        }
    }

    // Deletes specific weight entry for user
    fun deleteWeightEntry(userId: String, date: String, weight: Double) {
        writableDatabase.use { db ->
            db.delete(
                TABLE_DAILY_WEIGHT,
                "$COLUMN_USER_ID = ? AND $COLUMN_DATE = ? AND $COLUMN_WEIGHT = ?",
                arrayOf(userId, date, weight.toString())
            )
        }
    }

    // Inserts weight manually for a specific user
    fun insertDailyWeightManual(userId: String, customDate: String, weight: Double) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
                put(COLUMN_DATE, customDate)
                put(COLUMN_WEIGHT, weight)
            }
            db.insertWithOnConflict(TABLE_DAILY_WEIGHT, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // Retrieves all weight entries for user and sorts by date
    fun getAllDailyWeights(userId: String): List<DataGridItem> {
        return readableDatabase.use { db ->
            val dailyWeights = mutableListOf<DataGridItem>()
            val cursor = db.rawQuery(
                "SELECT $COLUMN_DATE, $COLUMN_WEIGHT FROM $TABLE_DAILY_WEIGHT WHERE $COLUMN_USER_ID = ? ORDER BY $COLUMN_DATE ASC",
                arrayOf(userId)
            )
            cursor.use {
                while (it.moveToNext()) {
                    val date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE))
                    val weight = it.getDouble(it.getColumnIndexOrThrow(COLUMN_WEIGHT))
                    dailyWeights.add(DataGridItem(date, weight))
                }
            }
            dailyWeights
        }
    }

    // Checks if user exists in database
    fun checkLogin(username: String, password: String): Boolean {
        return readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_LOGIN WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(username, password)
            )
            cursor.use { it.moveToFirst() && it.getInt(0) > 0 }
        }
    }

    // Inserts new user login entry and checks for duplicates
    fun insertLogin(context: Context, username: String, password: String): Boolean {
        return writableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_LOGIN WHERE $COLUMN_USERNAME = ?",
                arrayOf(username)
            )
            val userExists = cursor.use { it.moveToFirst() && it.getInt(0) > 0 }

            if (userExists) {
                Toast.makeText(context, "User already exists!", Toast.LENGTH_SHORT).show()
                return@use false
            }

            val values = ContentValues().apply {
                put(COLUMN_USERNAME, username)
                put(COLUMN_PASSWORD, password)
            }
            return db.insert(TABLE_LOGIN, null, values) != -1L
        }
    }

    // Exports user weight data from SQLite to Firestore
    fun exportDataToFirestore(context: Context) {
        val userId = auth.currentUser?.email ?: return

        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_DATE, $COLUMN_WEIGHT FROM $TABLE_DAILY_WEIGHT WHERE $COLUMN_USER_ID = ?",
            arrayOf(userId)
        )

        val weightData = mutableListOf<Map<String, Any>>()
        cursor.use {
            while (it.moveToNext()) {
                val date = it.getString(it.getColumnIndexOrThrow(COLUMN_DATE))
                val weight = it.getDouble(it.getColumnIndexOrThrow(COLUMN_WEIGHT))
                weightData.add(mapOf("date" to date, "weight" to weight))
            }
        }

        firestore.collection("users").document(userId)
            .set(mapOf("weights" to weightData))
            .addOnSuccessListener {
                Toast.makeText(context, "Data successfully exported to Firestore!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to export data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Imports user weight data from Firestore into SQLite
    fun importDataFromFirestore(context: Context) {
        val userId = auth.currentUser?.email ?: return
        Log.d("FirestoreImport", "Starting data import for user: $userId")

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("FirestoreImport", "Document exists, processing data...")

                    val weights = document.get("weights") as? List<*>
                    val parsedWeights = mutableListOf<Map<String, Any>>()
                    weights?.forEach { item ->
                        if (item is Map<*, *>) {
                            val mappedItem = item.entries.mapNotNull { (k, v) ->
                                if (v != null) k.toString() to v
                                else null
                            }.toMap()

                            parsedWeights.add(mappedItem)
                        }
                    }

                    if (parsedWeights.isEmpty()) {
                        Log.d("FirestoreImport", "No weight data found in Firestore.")
                    }

                    writableDatabase.use { _ ->
                        for (entry in parsedWeights) {
                            Log.d("FirestoreImport", "Importing weight entry: ${entry["date"]} - ${entry["weight"]}")
                            insertDailyWeightManual(userId, entry["date"].toString(), (entry["weight"] as Double))
                        }
                    }
                    Toast.makeText(context, "Data imported from Firestore!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("FirestoreImport", "No document found for user.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreImport", "Failed to import data: ${e.message}")
                Toast.makeText(context, "Failed to import data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 3
        private const val TABLE_DAILY_WEIGHT = "DailyWeight"
        private const val TABLE_GOAL_WEIGHT = "GoalWeight"
        private const val TABLE_LOGIN = "Login"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_WEIGHT = "weight"
        private const val COLUMN_GOAL_WEIGHT = "goalWeight"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
    }
}