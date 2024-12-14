package com.example.proyectofinaltdc

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import com.google.gson.Gson
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false

    private lateinit var buttonStart: Button
    private lateinit var textViewAirQuality: TextView
    private lateinit var textViewAirQualityStatus: TextView
    private lateinit var textViewNoiseLevel: TextView

    private val client = OkHttpClient()
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 2000  // 2000 ms = 2 seconds

    private lateinit var audioMonitor: AudioMonitor
    private var isMonitoring = false // Variable para controlar el estado de la medición

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Solicitar permiso de grabación
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )

        // Configurar las vistas y la lógica
        setupUI()
    }

    private fun setupUI() {
        buttonStart = findViewById(R.id.button_start)
        textViewAirQuality = findViewById(R.id.textViewAirQuality)
        textViewAirQualityStatus = findViewById(R.id.textViewAirQualityStatus)
        textViewNoiseLevel = findViewById(R.id.textViewNoiseLevel)

        audioMonitor = AudioMonitor()
        audioMonitor.onDecibelRead = { decibel ->
            val roundedDecibel = roundToThreeDecimals(decibel)
            runOnUiThread {
                textViewNoiseLevel.text = "Nivel de ruido: ${roundedDecibel}dB - ${classifyNoiseLevel(roundedDecibel)}"
            }
        }

        buttonStart.setOnClickListener {
            if (isMonitoring) {
                stopMonitoring()
                buttonStart.text = "Iniciar medición"
            } else {
                startMonitoring()
                buttonStart.text = "Detener medición"
            }
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        audioMonitor.startRecording(this) // Inicia la grabación del micrófono
        startRepeatingTask() // Inicia la recepción de datos de calidad del aire
    }

    private fun stopMonitoring() {
        isMonitoring = false
        audioMonitor.stopRecording() // Detiene la grabación del micrófono
        stopRepeatingTask() // Detiene la recepción de datos de calidad del aire
    }

    private fun roundToThreeDecimals(value: Double): Double {
        val df = DecimalFormat("#.###")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value).toDouble()
    }

    private fun classifyNoiseLevel(decibels: Double): String {
        return when {
            decibels < 50 -> "Silencio"
            decibels < 70 -> "Normal"
            decibels < 85 -> "Moderado"
            decibels < 100 -> "Alto"
            decibels < 120 -> "Muy alto"
            else -> "Extremadamente alto (Riesgo de daño auditivo)"
        }
    }

    private fun startRepeatingTask() {
        handler.postDelayed(runnable, updateInterval)
    }

    private fun stopRepeatingTask() {
        handler.removeCallbacks(runnable)
    }

    private val runnable = object : Runnable {
        override fun run() {
            fetchSensorData()
            if (isMonitoring) { // Asegura que solo se ejecute si está en modo de medición
                handler.postDelayed(this, updateInterval)
            }
        }
    }

    private fun fetchSensorData() {
        val request = Request.Builder()
            .url("http://3.215.122.64:8081/sensor/data") // Cambia la URL según tu configuración
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    textViewAirQuality.text = "Error de conexión: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread {
                            textViewAirQuality.text = "Respuesta no exitosa: ${it.code}"
                        }
                    } else {
                        val responseData = it.body?.string()
                        if (responseData != null) {
                            val airQualityData = gson.fromJson(responseData, Array<AirQualityData>::class.java).last()
                            runOnUiThread {
                                updateUI(airQualityData)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun updateUI(data: AirQualityData) {
        textViewAirQuality.text = "PM2.5: ${data.pm25_standard}, PM10: ${data.pm10_standard}"
        textViewAirQualityStatus.text = "Estado PM2.5: ${data.calidad_pm25}, Estado PM10: ${data.calidad_pm10}"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (!permissionToRecordAccepted) {
            finish() // Salir de la aplicación si no se conceden los permisos
        }
    }
}

data class AirQualityData(
    val pm10_standard: Int,
    val pm25_standard: Int,
    val calidad_pm25: String,
    val calidad_pm10: String
)
