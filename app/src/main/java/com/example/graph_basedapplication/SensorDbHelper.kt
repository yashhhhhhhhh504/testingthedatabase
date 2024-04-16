package com.example.graph_basedapplication

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SensorDbHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private var instance: SensorDbHelper? = null

        fun getInstance(context: Context): SensorDbHelper {
            return instance ?: synchronized(this) {
                instance ?: SensorDbHelper(context.applicationContext).also { instance = it }
            }
        }

        const val DATABASE_NAME = "sensor.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "readings"
        const val COLUMN_ID = "id"
        const val COLUMN_AZIMUTH = "azimuth"
        const val COLUMN_PITCH = "pitch"
        const val COLUMN_ROLL = "roll"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_AZIMUTH REAL,
                $COLUMN_PITCH REAL,
                $COLUMN_ROLL REAL
            )
        """.trimIndent()
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertReading(azimuth: Float, pitch: Float, roll: Float) {
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_AZIMUTH, azimuth)
                put(COLUMN_PITCH, pitch)
                put(COLUMN_ROLL, roll)
            }
            db.insert(TABLE_NAME, null, values)
        } finally {
            db.close()
        }
    }

    fun getLastReading(): Triple<Float, Float, Float>? {
        val db = readableDatabase
        var result: Triple<Float, Float, Float>? = null
        try {
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COLUMN_AZIMUTH, COLUMN_PITCH, COLUMN_ROLL),
                null,
                null,
                null,
                null,
                "$COLUMN_ID DESC",
                "1"
            )
            if (cursor.moveToFirst()) {
                val azimuthIndex = cursor.getColumnIndexOrThrow(COLUMN_AZIMUTH)
                val pitchIndex = cursor.getColumnIndexOrThrow(COLUMN_PITCH)
                val rollIndex = cursor.getColumnIndexOrThrow(COLUMN_ROLL)
                val azimuth = cursor.getFloat(azimuthIndex)
                val pitch = cursor.getFloat(pitchIndex)
                val roll = cursor.getFloat(rollIndex)
                result = Triple(azimuth, pitch, roll)
            }
            cursor.close()
        } finally {
            db.close()
        }
        return result
    }
}
