package com.moovclone.app.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _cadence = MutableStateFlow(0f)
    val cadence: StateFlow<Float> = _cadence.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var lastStepTime = 0L
    private var stepCounterOffset = 0

    fun startListening() {
        if (!_isListening.value) {
            stepCounterSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            accelerometerSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            _isListening.value = true
        }
    }

    fun stopListening() {
        if (_isListening.value) {
            sensorManager.unregisterListener(this)
            _isListening.value = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    if (stepCounterOffset == 0) {
                        stepCounterOffset = it.values[0].toInt()
                    }
                    _stepCount.value = it.values[0].toInt() - stepCounterOffset
                    calculateCadence()
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    calculateMovementQuality(it.values)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateCadence() {
        val currentTime = System.currentTimeMillis()
        if (lastStepTime > 0) {
            val timeDiff = currentTime - lastStepTime
            if (timeDiff > 0) {
                _cadence.value = 60000f / timeDiff
            }
        }
        lastStepTime = currentTime
    }

    private fun calculateMovementQuality(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Evaluate impact and form based on acceleration
        if (magnitude > 20) {
            // High impact - poor form or jumping
        }
    }

    fun reset() {
        stepCounterOffset = 0
        _stepCount.value = 0
        lastStepTime = 0L
    }
}
