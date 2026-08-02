package com.moovclone.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moovclone.app.manager.AudioCoachManager
import com.moovclone.app.manager.BluetoothManagerImpl
import com.moovclone.app.manager.StepSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer

data class WorkoutState(
    val isRunning: Boolean = false,
    val elapsedTime: Long = 0,
    val stepCount: Int = 0,
    val cadence: Float = 0f,
    val distance: Float = 0f,
    val calories: Float = 0f,
    val heartRate: Int = 0,
    val isPlayingMusic: Boolean = false,
    val musicVolume: Float = 0.5f
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val audioCoachManager = AudioCoachManager(application)
    private val bluetoothManager = BluetoothManagerImpl(application)
    private val sensorManager = StepSensorManager(application)

    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _pairedDevices = bluetoothManager.pairedDevices
    val pairedDevices = _pairedDevices

    private val _connectedDevice = bluetoothManager.connectedDevice
    val connectedDevice = _connectedDevice

    private val _stepCount = sensorManager.stepCount
    val stepCount = _stepCount

    private val _cadence = sensorManager.cadence
    val cadence = _cadence

    private var workoutTimer: Timer? = null
    private var feedbackTimer: Timer? = null

    init {
        bluetoothManager.updatePairedDevices()
        viewModelScope.launch {
            sensorManager.stepCount.collect { steps ->
                _workoutState.value = _workoutState.value.copy(stepCount = steps)
                updateDistance()
                updateCalories()
            }
        }
        viewModelScope.launch {
            sensorManager.cadence.collect { cad ->
                _workoutState.value = _workoutState.value.copy(cadence = cad)
            }
        }
    }

    fun startWorkout() {
        _workoutState.value = _workoutState.value.copy(isRunning = true)
        sensorManager.startListening()
        sensorManager.reset()

        workoutTimer = timer(initialDelay = 0, period = 1000) {
            _workoutState.value = _workoutState.value.copy(
                elapsedTime = _workoutState.value.elapsedTime + 1000
            )
        }

        feedbackTimer = timer(initialDelay = 10000, period = 30000) {
            if (_workoutState.value.isRunning) {
                val feedbackText = generateFeedback()
                audioCoachManager.speak(feedbackText)
            }
        }

        audioCoachManager.speak("Workout started. Let's go!")
    }

    fun stopWorkout() {
        _workoutState.value = _workoutState.value.copy(isRunning = false)
        workoutTimer?.cancel()
        feedbackTimer?.cancel()
        sensorManager.stopListening()
        audioCoachManager.speak("Workout completed. Great job!")
    }

    fun pauseWorkout() {
        _workoutState.value = _workoutState.value.copy(isRunning = false)
        workoutTimer?.cancel()
        feedbackTimer?.cancel()
        sensorManager.stopListening()
    }

    fun resumeWorkout() {
        _workoutState.value = _workoutState.value.copy(isRunning = true)
        sensorManager.startListening()
        startWorkout()
    }

    fun toggleMusic() {
        if (_workoutState.value.isPlayingMusic) {
            audioCoachManager.pauseMusic()
        } else {
            audioCoachManager.playMusic()
        }
        _workoutState.value = _workoutState.value.copy(
            isPlayingMusic = !_workoutState.value.isPlayingMusic
        )
    }

    fun setMusicVolume(volume: Float) {
        _workoutState.value = _workoutState.value.copy(musicVolume = volume)
    }

    fun connectBluetoothDevice(deviceName: String) {
        val device = bluetoothManager.pairedDevices.value.firstOrNull { it.name == deviceName }
        device?.let {
            bluetoothManager.connectToDevice(it)
            audioCoachManager.speak("Connected to $deviceName")
        }
    }

    fun disconnectBluetoothDevice() {
        bluetoothManager.disconnectDevice()
        audioCoachManager.speak("Disconnected")
    }

    fun scanBluetoothDevices() {
        bluetoothManager.startScanning()
    }

    fun stopBluetoothScan() {
        bluetoothManager.stopScanning()
    }

    private fun updateDistance() {
        val distance = _workoutState.value.stepCount * 0.762f / 1000f
        _workoutState.value = _workoutState.value.copy(distance = distance)
    }

    private fun updateCalories() {
        val calories = _workoutState.value.stepCount * 0.04f
        _workoutState.value = _workoutState.value.copy(calories = calories)
    }

    private fun generateFeedback(): String {
        return when {
            _workoutState.value.cadence < 160 -> "Cadence is low. Try to increase your pace."
            _workoutState.value.cadence > 180 -> "Perfect cadence! Keep it up."
            _workoutState.value.stepCount > 5000 -> "You are doing great! More than 5000 steps."
            _workoutState.value.stepCount > 2000 -> "Good progress! Keep running."
            else -> "Keep going! You are doing well."
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        audioCoachManager.release()
        workoutTimer?.cancel()
        feedbackTimer?.cancel()
    }
}
