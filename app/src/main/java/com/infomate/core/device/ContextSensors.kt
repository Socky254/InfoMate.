package com.infomate.core.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.BatteryManager
import android.util.Log
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.math.abs

class ContextSensors(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var currentLight: Float = 0f
    private var currentMotion: Float = 0f
    
    private var audioRecord: AudioRecord? = null
    private var isMonitoringAudio = false
    private var lastAcousticLevel = 0.0
    private var sampleBuffer = ShortArray(1024)
    
    init {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        startAudioMonitoring()
    }

    private fun startAudioMonitoring() {
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
            
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isMonitoringAudio = true
                thread {
                    while (isMonitoringAudio) {
                        val read = audioRecord?.read(sampleBuffer, 0, sampleBuffer.size) ?: 0
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                sum += abs(sampleBuffer[i].toDouble())
                            }
                            lastAcousticLevel = sum / read
                            // FFT logic would go here to detect "Music" vs "Noise"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContextSensors", "Audio monitoring failed: ${e.message}")
        }
    }

    fun getAcousticLevel(): Double = lastAcousticLevel

    fun isUserActive(): Boolean {
        // Active if there is motion (> 10.0 is roughly moving) or light (> 50 lux)
        return currentMotion > 10.5f || currentLight > 20f
    }

    fun getSensoryContext(): SensoryContext {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        Log.i("ContextSensors", "Capturing environmental telemetry...")
        
        return SensoryContext(
            ambientLight = currentLight,
            acousticNoiseLevel = getAcousticLevel(),
            deviceEnergyState = batteryLevel,
            geospatialVector = "LAT:0.0 | LON:0.0", // Mocked for privacy until GPS enabled
            electromagneticFrequency = 2.4f // Simulated GHz
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_LIGHT -> currentLight = event.values[0]
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                currentMotion = kotlin.math.sqrt(x*x + y*y + z*z)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun shutdown() {
        sensorManager.unregisterListener(this)
        isMonitoringAudio = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }
}
