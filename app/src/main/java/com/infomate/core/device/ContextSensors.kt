package com.infomate.core.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.Log

class ContextSensors(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var currentLight: Float = 0f
    
    init {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun getSensoryContext(): SensoryContext {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        Log.i("ContextSensors", "Capturing environmental telemetry...")
        
        return SensoryContext(
            ambientLight = currentLight,
            deviceEnergyState = batteryLevel,
            geospatialVector = "LAT:0.0 | LON:0.0", // Mocked for privacy until GPS enabled
            electromagneticFrequency = 2.4f // Simulated GHz
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentLight = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
