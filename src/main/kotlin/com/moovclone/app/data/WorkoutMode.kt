package com.moovclone.app.data

enum class WorkoutMode(val displayName: String, val description: String) {
    RUNNING("Laufen", "Tempo, Kadenz & Herzfrequenz gesteuert"),
    WALKING("Gehen", "Langsames, entspanntes Gehen"),
    SWIMMING("Schwimmen", "Mit Takterkennung & Züge-Zähler"),
    BODYWEIGHT("Bodyweight Circuit", "Wiederholungen & HIIT-Training"),
    CYCLING("Radfahren", "Indoor & Outdoor mit Trittfrequenz"),
    CARDIO_BOXING("Cardio Boxing", "Rhythmus-basiertes Boxen-Training"),
}

data class WorkoutSession(
    val mode: WorkoutMode = WorkoutMode.RUNNING,
    val elapsedTime: Long = 0,
    val stepCount: Int = 0,
    val cadence: Float = 0f,
    val heartRate: Int = 0,
    val distance: Float = 0f,
    val calories: Float = 0f,
    val isRunning: Boolean = false,
    val isPlayingMusic: Boolean = false,
    val musicVolume: Float = 0.5f,
    val isConnected: Boolean = false,
    // Running specific
    val currentZone: HeartRateZone = HeartRateZone.ZONE_1,
    // Swimming specific
    val strokeCount: Int = 0,
    val poolLength: Float = 50f,
    val swimDistance: Float = 0f,
    // Bodyweight specific
    val repCount: Int = 0,
    val setCount: Int = 0,
    val exerciseName: String = "",
    val restTime: Long = 0,
    // Cycling specific
    val rpm: Float = 0f,
    val power: Float = 0f,
    // Cardio Boxing specific
    val punchCount: Int = 0,
    val comboCount: Int = 0,
    val roundNumber: Int = 1,
)

enum class HeartRateZone(val minHR: Int, val maxHR: Int, val name: String) {
    ZONE_1(100, 120, "Zone 1 - Warm-up"),
    ZONE_2(120, 140, "Zone 2 - Base"),
    ZONE_3(140, 160, "Zone 3 - Tempo"),
    ZONE_4(160, 180, "Zone 4 - Threshold"),
    ZONE_5(180, 200, "Zone 5 - Max"),
}

fun getHeartRateZone(hr: Int, maxHR: Int = 200): HeartRateZone {
    val percentage = (hr.toFloat() / maxHR) * 100
    return when {
        percentage < 60 -> HeartRateZone.ZONE_1
        percentage < 70 -> HeartRateZone.ZONE_2
        percentage < 80 -> HeartRateZone.ZONE_3
        percentage < 90 -> HeartRateZone.ZONE_4
        else -> HeartRateZone.ZONE_5
    }
}
