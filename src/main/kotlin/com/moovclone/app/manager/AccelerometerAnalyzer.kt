package com.moovclone.app.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.moovclone.app.data.WorkoutMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class AccelerometerAnalyzer(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _movementMagnitude = MutableStateFlow(0f)
    val movementMagnitude: StateFlow<Float> = _movementMagnitude.asStateFlow()

    private val _repDetected = MutableStateFlow(false)
    val repDetected: StateFlow<Boolean> = _repDetected.asStateFlow()

    private val _strokeDetected = MutableStateFlow(false)
    val strokeDetected: StateFlow<Boolean> = _strokeDetected.asStateFlow()

    private val _punchDetected = MutableStateFlow(false)
    val punchDetected: StateFlow<Boolean> = _punchDetected.asStateFlow()

    private val _stepImpact = MutableStateFlow(0f)
    val stepImpact: StateFlow<Float> = _stepImpact.asStateFlow()

    // Bewegungs-Historie für Muster-Erkennung
    private val accelerationHistory = mutableListOf<Float>()
    private val maxHistorySize = 50

    // Schwellenwerte für verschiedene Aktivitäten
    private val PUNCH_THRESHOLD = 35f
    private val REP_THRESHOLD = 25f
    private val STROKE_THRESHOLD = 30f
    private val STEP_IMPACT_THRESHOLD = 20f

    // Timing für Debouncing
    private var lastPunchTime = 0L
    private var lastRepTime = 0L
    private var lastStrokeTime = 0L
    private var lastStepTime = 0L

    private val PUNCH_DEBOUNCE_MS = 150L
    private val REP_DEBOUNCE_MS = 500L
    private val STROKE_DEBOUNCE_MS = 200L
    private val STEP_DEBOUNCE_MS = 300L

    // Bewegungs-Richtung für Erkennung
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f

    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            // Berechne Gesamtbeschleunigung (minus Schwerkraft ~9.8)
            val magnitude = sqrt(x * x + y * y + z * z) - 9.8f

            _movementMagnitude.value = magnitude

            // Speichere in History für Trend-Analyse
            accelerationHistory.add(magnitude)
            if (accelerationHistory.size > maxHistorySize) {
                accelerationHistory.removeAt(0)
            }

            // Erkenne Bewegungsmuster basierend auf Magnitude
            detectPunch(magnitude, x, y, z)
            detectRep(magnitude)
            detectStroke(magnitude)
            detectStepImpact(magnitude)

            lastAccelX = x
            lastAccelY = y
            lastAccelZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Punch-Erkennung: Schnelle, zielgerichtete Beschleunigung
     * Typisch: 40-60m/s² in Millisekunden
     */
    private fun detectPunch(magnitude: Float, x: Float, y: Float, z: Float) {
        val currentTime = System.currentTimeMillis()

        // Erkenne schnelle Bewegung in einer Richtung
        val horizontalAccel = sqrt(x * x + y * y)

        if (magnitude > PUNCH_THRESHOLD && horizontalAccel > 30f) {
            if (currentTime - lastPunchTime > PUNCH_DEBOUNCE_MS) {
                _punchDetected.value = true
                lastPunchTime = currentTime

                // Reset nach kurzer Zeit
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    _punchDetected.value = false
                }, 100L)
            }
        }
    }

    /**
     * Rep-Erkennung: Oszillierende Bewegung (auf-ab)
     * Typisch: Armbeweung in Z-Richtung mit ~1-2Hz
     */
    private fun detectRep(magnitude: Float) {
        val currentTime = System.currentTimeMillis()

        // Erkenne Oszillation in der Historie
        if (accelerationHistory.size > 20) {
            val recentMagnitudes = accelerationHistory.takeLast(20)
            val hasPeak = recentMagnitudes.any { it > REP_THRESHOLD }
            val hasValley = recentMagnitudes.any { it < -REP_THRESHOLD }

            // Wenn es Peaks und Valleys gibt = Oszillation = Rep
            if (hasPeak && hasValley && magnitude > REP_THRESHOLD) {
                if (currentTime - lastRepTime > REP_DEBOUNCE_MS) {
                    _repDetected.value = true
                    lastRepTime = currentTime

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        _repDetected.value = false
                    }, 200L)
                }
            }
        }
    }

    /**
     * Stroke-Erkennung: Wiederholte, rhythmische Bewegung
     * Typisch: Schwimmen, Rudern - kontinuierliche Oszillation
     */
    private fun detectStroke(magnitude: Float) {
        val currentTime = System.currentTimeMillis()

        // Erkenne repetitive Muster
        if (accelerationHistory.size > 15) {
            val recent = accelerationHistory.takeLast(15)
            val peaks = recent.count { it > STROKE_THRESHOLD }
            val avgMagnitude = recent.average()

            // Wenn viele Peaks in kurzer Zeit = wiederholte Bewegung
            if (peaks > 3 && avgMagnitude > 15f) {
                if (currentTime - lastStrokeTime > STROKE_DEBOUNCE_MS) {
                    _strokeDetected.value = true
                    lastStrokeTime = currentTime

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        _strokeDetected.value = false
                    }, 150L)
                }
            }
        }
    }

    /**
     * Schritt-Impact-Erkennung: Vertikale Stoßbelastung
     * Typisch beim Laufen: Z-Achsen-Spitzen
     */
    private fun detectStepImpact(magnitude: Float) {
        val currentTime = System.currentTimeMillis()

        if (magnitude > STEP_IMPACT_THRESHOLD) {
            _stepImpact.value = magnitude

            if (currentTime - lastStepTime > STEP_DEBOUNCE_MS) {
                // Markiere schweren Schritt (schlechte Form)
                if (magnitude > 35f) {
                    lastStepTime = currentTime
                }
            }
        }
    }

    /**
     * Erkenne Trainingsmodus basierend auf Bewegungs-Muster
     */
    fun detectWorkoutMode(): WorkoutMode? {
        if (accelerationHistory.size < 50) return null

        val avgMagnitude = accelerationHistory.average()
        val peaks = accelerationHistory.count { it > 25f }
        val frequency = peaks / (accelerationHistory.size / 50f)

        return when {
            // Cardio Boxing: Sehr schnelle, impulsive Bewegungen
            frequency > 5 && avgMagnitude > 20f -> WorkoutMode.CARDIO_BOXING

            // Bodyweight: Moderate, rhythmische Oszillation
            frequency in 2f..4f && avgMagnitude in 15f..25f -> WorkoutMode.BODYWEIGHT

            // Swimming: Kontinuierliche, gleichmäßige Bewegung
            frequency in 1f..2f && avgMagnitude in 18f..28f -> WorkoutMode.SWIMMING

            // Running: Regelmäßige vertikale Stoßbelastung
            frequency in 2f..3f && avgMagnitude in 12f..20f -> WorkoutMode.RUNNING

            // Cycling: Gleichmäßige, kreisförmige Bewegung
            frequency < 1f && avgMagnitude in 10f..18f -> WorkoutMode.CYCLING

            else -> null
        }
    }

    /**
     * Gib Feedback zum Bewegungs-Qualität
     */
    fun getFormFeedback(mode: WorkoutMode): String? {
        if (accelerationHistory.size < 20) return null

        val avgMagnitude = accelerationHistory.average()
        val maxMagnitude = accelerationHistory.maxOrNull() ?: 0f

        return when (mode) {
            WorkoutMode.RUNNING -> {
                when {
                    maxMagnitude > 40f -> "Zu schwerer Aufprall - lander weicher"
                    avgMagnitude < 10f -> "Zu wenig Kraft - push harder"
                    else -> "Gute Laufform!"
                }
            }

            WorkoutMode.BODYWEIGHT -> {
                when {
                    maxMagnitude > 35f -> "Zu schnell - kontrolliere die Bewegung"
                    avgMagnitude < 15f -> "Zu langsam - schnellere Reps"
                    else -> "Ausgezeichnete Technik!"
                }
            }

            WorkoutMode.SWIMMING -> {
                when {
                    maxMagnitude > 35f -> "Zu aggressiv - gleichmäßiger schwimmen"
                    avgMagnitude < 15f -> "Zu schwache Züge - kräftiger ziehen"
                    else -> "Perfekter Rhythmus!"
                }
            }

            WorkoutMode.CYCLING -> {
                when {
                    maxMagnitude > 25f -> "Zu unruhig - gleichmäßiger treten"
                    avgMagnitude < 8f -> "Zu niedriger Widerstand - RPM erhöhen"
                    else -> "Gute Kadenz!"
                }
            }

            WorkoutMode.CARDIO_BOXING -> {
                when {
                    maxMagnitude > 50f -> "Explosive Schläge! Weiter so!"
                    avgMagnitude < 20f -> "Schläge sind zu schwach"
                    else -> "Gutes Tempo halten!"
                }
            }

            else -> null
        }
    }

    /**
     * Berechne Schlag-Kraftlevel (0-100)
     */
    fun getPunchPowerLevel(): Int {
        val recentPunches = accelerationHistory.takeLast(10).filter { it > PUNCH_THRESHOLD }
        if (recentPunches.isEmpty()) return 0

        val avgPower = recentPunches.average()
        return ((avgPower / 60f) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Berechne durchschnittliche Bewegungs-Konsistenz (0-100)
     */
    fun getConsistency(): Int {
        if (accelerationHistory.size < 10) return 0

        val avg = accelerationHistory.average()
        val variance = accelerationHistory.map { (it - avg) * (it - avg) }.average()
        val standardDev = sqrt(variance)

        // Niedrige Standardabweichung = höhere Konsistenz
        val consistency = (100 - (standardDev * 5)).toInt()
        return consistency.coerceIn(0, 100)
    }
}
