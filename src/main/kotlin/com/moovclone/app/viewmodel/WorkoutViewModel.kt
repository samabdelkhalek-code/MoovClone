package com.moovclone.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moovclone.app.data.WorkoutMode
import com.moovclone.app.data.HeartRateZone
import com.moovclone.app.data.getHeartRateZone
import com.moovclone.app.manager.AccelerometerAnalyzer
import com.moovclone.app.manager.AudioCoachManager
import com.moovclone.app.manager.BluetoothManagerImpl
import com.moovclone.app.manager.CoachingEngine
import com.moovclone.app.manager.MusicManager
import com.moovclone.app.manager.StepSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer

data class WorkoutState(
    val mode: WorkoutMode = WorkoutMode.RUNNING,
    val isRunning: Boolean = false,
    val elapsedTime: Long = 0,
    val stepCount: Int = 0,
    val cadence: Float = 0f,
    val distance: Float = 0f,
    val calories: Float = 0f,
    val heartRate: Int = 0,
    val isPlayingMusic: Boolean = false,
    val musicVolume: Float = 0.5f,
    val isConnected: Boolean = false,
    val form: String = "neutral",
    // Mode-specific
    val currentZone: HeartRateZone = HeartRateZone.ZONE_1,
    val strokeCount: Int = 0,
    val poolLength: Float = 50f,
    val swimDistance: Float = 0f,
    val repCount: Int = 0,
    val setCount: Int = 0,
    val exerciseName: String = "Push-ups",
    val restTime: Long = 0,
    val rpm: Float = 0f,
    val power: Float = 0f,
    val punchCount: Int = 0,
    val comboCount: Int = 0,
    val roundNumber: Int = 1,
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val audioCoachManager = AudioCoachManager(application)
    private val bluetoothManager = BluetoothManagerImpl(application)
    private val sensorManager = StepSensorManager(application)
    private val accelerometerAnalyzer = AccelerometerAnalyzer(application)
    private val coachingEngine = CoachingEngine()
    val musicManager = MusicManager(application)

    val punchDetected = accelerometerAnalyzer.punchDetected
    val repDetected = accelerometerAnalyzer.repDetected
    val strokeDetected = accelerometerAnalyzer.strokeDetected
    val movementMagnitude = accelerometerAnalyzer.movementMagnitude

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
        viewModelScope.launch {
            bluetoothManager.heartRate.collect { hr ->
                _workoutState.value = _workoutState.value.copy(heartRate = hr)
            }
        }
        viewModelScope.launch {
            bluetoothManager.connectedDevice.collect { device ->
                _workoutState.value = _workoutState.value.copy(isConnected = device != null)
            }
        }
        viewModelScope.launch {
            accelerometerAnalyzer.punchDetected.collect { detected ->
                if (detected && _workoutState.value.mode == WorkoutMode.CARDIO_BOXING) {
                    val newCount = _workoutState.value.punchCount + 1
                    _workoutState.value = _workoutState.value.copy(punchCount = newCount)
                    if (newCount % 10 == 0) {
                        audioCoachManager.speak("${newCount} Schläge!")
                    }
                }
            }
        }
        viewModelScope.launch {
            accelerometerAnalyzer.repDetected.collect { detected ->
                if (detected && _workoutState.value.mode == WorkoutMode.BODYWEIGHT) {
                    val newCount = _workoutState.value.repCount + 1
                    _workoutState.value = _workoutState.value.copy(repCount = newCount)
                    if (newCount % 10 == 0) {
                        audioCoachManager.speak("${newCount} Wiederholungen!")
                    }
                }
            }
        }
        viewModelScope.launch {
            accelerometerAnalyzer.strokeDetected.collect { detected ->
                if (detected && _workoutState.value.mode == WorkoutMode.SWIMMING) {
                    val newCount = _workoutState.value.strokeCount + 1
                    _workoutState.value = _workoutState.value.copy(strokeCount = newCount)
                }
            }
        }
    }

    fun startWorkout() {
        _workoutState.value = _workoutState.value.copy(isRunning = true)
        sensorManager.startListening()
        sensorManager.reset()
        accelerometerAnalyzer.startListening()

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
        accelerometerAnalyzer.stopListening()
        audioCoachManager.speak("Workout completed. Great job!")
    }

    fun pauseWorkout() {
        _workoutState.value = _workoutState.value.copy(isRunning = false)
        workoutTimer?.cancel()
        feedbackTimer?.cancel()
        sensorManager.stopListening()
        accelerometerAnalyzer.stopListening()
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
        val state = _workoutState.value
        val zone = getHeartRateZone(state.heartRate)
        _workoutState.value = _workoutState.value.copy(currentZone = zone)

        return when (state.mode) {
            WorkoutMode.RUNNING -> {
                coachingEngine.generateRunningFeedback(
                    state.stepCount,
                    state.cadence,
                    state.heartRate,
                    state.elapsedTime
                ) ?: coachingEngine.getMotivationalPhrase()
            }
            WorkoutMode.WALKING -> {
                coachingEngine.generateRunningFeedback(
                    state.stepCount,
                    state.cadence * 0.8f,
                    state.heartRate,
                    state.elapsedTime
                ) ?: "Gutes Gehempo!"
            }
            WorkoutMode.SWIMMING -> {
                coachingEngine.generateSwimmingFeedback(
                    state.strokeCount,
                    state.swimDistance,
                    state.heartRate,
                    state.poolLength
                ) ?: "Bleib im Rhythmus!"
            }
            WorkoutMode.BODYWEIGHT -> {
                coachingEngine.generateBodyweightFeedback(
                    state.repCount,
                    state.setCount,
                    state.heartRate,
                    state.exerciseName
                ) ?: "Weiter so!"
            }
            WorkoutMode.CYCLING -> {
                coachingEngine.generateCyclingFeedback(
                    state.rpm,
                    state.power,
                    state.heartRate
                ) ?: "Gutes Tempo halten!"
            }
            WorkoutMode.CARDIO_BOXING -> {
                coachingEngine.generateCardioBoxingFeedback(
                    state.punchCount,
                    state.comboCount,
                    state.heartRate,
                    state.roundNumber
                ) ?: "Keep punching!"
            }
        }
    }

    fun setWorkoutMode(mode: WorkoutMode) {
        _workoutState.value = _workoutState.value.copy(mode = mode)
    }

    fun incrementRepCount() {
        _workoutState.value = _workoutState.value.copy(
            repCount = _workoutState.value.repCount + 1
        )
    }

    fun incrementSetCount() {
        _workoutState.value = _workoutState.value.copy(
            setCount = _workoutState.value.setCount + 1,
            repCount = 0
        )
    }

    fun incrementStrokeCount() {
        _workoutState.value = _workoutState.value.copy(
            strokeCount = _workoutState.value.strokeCount + 1
        )
    }

    fun incrementPunchCount() {
        _workoutState.value = _workoutState.value.copy(
            punchCount = _workoutState.value.punchCount + 1
        )
    }

    fun incrementCombo() {
        _workoutState.value = _workoutState.value.copy(
            comboCount = _workoutState.value.comboCount + 1
        )
    }

    fun nextRound() {
        _workoutState.value = _workoutState.value.copy(
            roundNumber = _workoutState.value.roundNumber + 1,
            punchCount = 0
        )
    }

    fun setExerciseName(name: String) {
        _workoutState.value = _workoutState.value.copy(exerciseName = name)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        accelerometerAnalyzer.stopListening()
        audioCoachManager.release()
        musicManager.release()
        workoutTimer?.cancel()
        feedbackTimer?.cancel()
    }
}
