package com.example.graph_basedapplication
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.graph_basedapplication.ui.theme.Graph_basedapplicationTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var dbHelper: SensorDbHelper
    private var accelerometer: Sensor? = null

    private var azimuth = mutableStateOf(0f)
    private var pitch = mutableStateOf(0f)
    private var roll = mutableStateOf(0f)

    private val dbScope = CoroutineScope(Dispatchers.IO)  // Coroutine scope for IO-bound tasks like database operations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        dbHelper = SensorDbHelper.getInstance(this)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: throw IllegalStateException("Sensor not available")

        setContent {
            Graph_basedapplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OrientationDisplay(
                        azimuth.value, pitch.value, roll.value,
                        onStartClick = { startSensorReading() },
                        onShowAnglesClick = { showCurrentAngles() }
                    )
                }
            }
        }
    }

    private fun startSensorReading() {
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun showCurrentAngles() {
        dbScope.launch {
            val lastReading = dbHelper.getLastReading()
            withContext(Dispatchers.Main) {
                lastReading?.let {
                    azimuth.value = it.first
                    pitch.value = it.second
                    roll.value = it.third
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startSensorReading()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                azimuth.value = it.values[0]
                pitch.value = it.values[1]
                roll.value = it.values[2]
                insertReadingAsync(azimuth.value, pitch.value, roll.value)
            }
        }
    }

    private fun insertReadingAsync(azimuth: Float, pitch: Float, roll: Float) {
        dbScope.launch {
            try {
                dbHelper.insertReading(azimuth, pitch, roll)
            } catch (e: Exception) {
                // Log or handle database exception here
                e.printStackTrace()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // This method is not used in this example.
    }

    @Composable
    fun OrientationDisplay(azimuth: Float, pitch: Float, roll: Float, onStartClick: () -> Unit, onShowAnglesClick: () -> Unit) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Azimuth: $azimuth")
            Text(text = "Pitch: $pitch")
            Text(text = "Roll: $roll")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onStartClick) {
                Text("Start Reading Angles")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onShowAnglesClick) {
                Text("Show Current Angles")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        Graph_basedapplicationTheme {
            OrientationDisplay(azimuth.value, pitch.value, roll.value, {}, {})
        }
    }
}
