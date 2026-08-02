package com.moovclone.app.manager

import com.moovclone.app.data.HeartRateZone
import com.moovclone.app.data.WorkoutMode
import com.moovclone.app.data.getHeartRateZone

class CoachingEngine {

    fun generateRunningFeedback(
        steps: Int,
        cadence: Float,
        heartRate: Int,
        elapsedTime: Long
    ): String? {
        return when {
            cadence < 150 && steps > 100 -> "Erhöhen Sie Ihr Tempo! Ziel: 160 Schritte pro Minute"
            cadence > 190 && heartRate < 140 -> "Gutes Tempo! Beibehalten"
            heartRate > 180 -> "Herzfrequenz zu hoch - reduzieren Sie das Tempo"
            heartRate < 100 && steps > 500 -> "Erhöhen Sie die Intensität für bessere Ergebnisse"
            steps % 1000 == 0 && steps > 0 -> "${steps} Schritte! Weiter so"
            (elapsedTime / 1000) % 300 == 0L && elapsedTime > 0 -> "Gute Form! Halten Sie dieses Tempo"
            else -> null
        }
    }

    fun generateSwimmingFeedback(
        strokes: Int,
        swimDistance: Float,
        heartRate: Int,
        poolLength: Float = 50f
    ): String? {
        return when {
            strokes > 0 && strokes % 50 == 0 -> "${strokes} Züge! ${String.format("%.0f", swimDistance)}m geschwommen"
            swimDistance > 0 && swimDistance % poolLength == 0f -> "Länge ${(swimDistance / poolLength).toInt()} abgeschlossen!"
            heartRate > 170 -> "Gute Intensität! Weiter so"
            heartRate < 100 && strokes > 100 -> "Erhöhen Sie die Geschwindigkeit"
            strokes == 1 -> "Guter Start! Bleiben Sie im Rhythmus"
            else -> null
        }
    }

    fun generateBodyweightFeedback(
        reps: Int,
        sets: Int,
        heartRate: Int,
        exerciseName: String
    ): String? {
        return when {
            reps > 0 && reps % 10 == 0 -> "$reps Wiederholungen von $exerciseName! Ausgezeichnet!"
            sets > 0 && reps == 1 -> "Set $sets gestartet! Gute Form"
            heartRate > 175 -> "Herzfrequenz im Zone 4! Bleiben Sie fokussiert"
            heartRate < 120 && reps > 30 -> "Intensität erhöhen für bessere Ergebnisse"
            reps == 5 -> "Nur noch $reps Wiederholungen!"
            reps == 1 -> "Letzte Wiederholung! Push!"
            else -> null
        }
    }

    fun generateCyclingFeedback(
        rpm: Float,
        power: Float,
        heartRate: Int
    ): String? {
        return when {
            rpm < 80 && power > 200 -> "Trittfrequenz erhöhen - langsamer, aber höhere Drehzahl"
            rpm > 120 && power < 150 -> "Widerstand erhöhen - höhere Kraft brauchen wir"
            heartRate > 185 -> "HR Zone 5! Reduzieren Sie die Intensität"
            rpm in 85..110 && power in 150..250 -> "Perfekte Zone! Beibehalten"
            power > 300 && rpm in 80..100 -> "Ausgezeichnet! Hohe Leistung"
            else -> null
        }
    }

    fun generateCardioBoxingFeedback(
        punches: Int,
        combos: Int,
        heartRate: Int,
        roundNumber: Int
    ): String? {
        return when {
            punches > 0 && punches % 50 == 0 -> "${punches} Schläge! Gutes Tempo!"
            combos > 0 && combos % 10 == 0 -> "Combo #${combos}! Schöne Sequenzen!"
            roundNumber > 1 && punches < 20 -> "Neue Runde! Warme dich auf"
            heartRate > 180 -> "Maximale Intensität! Push!"
            heartRate in 150..170 -> "Perfekte Boxing-Zone!"
            punches == 0 && heartRate > 100 -> "Erste Schläge kommen - go!"
            else -> null
        }
    }

    fun getZoneCoaching(zone: HeartRateZone): String {
        return when (zone) {
            HeartRateZone.ZONE_1 -> "Warm-up Zone - Lockeres Tempo, Beine wärmen"
            HeartRateZone.ZONE_2 -> "Basis-Zone - Gemächliches Tempo, lange durchhalten"
            HeartRateZone.ZONE_3 -> "Tempo-Zone - Zügiges Tempo, intensive Arbeit"
            HeartRateZone.ZONE_4 -> "Schwellen-Zone - Sehr anstrengend, nur kurze Zeit"
            HeartRateZone.ZONE_5 -> "Max-Zone - Maximale Anstrengung! Kurze Sprints"
        }
    }

    fun getMotivationalPhrase(): String {
        val phrases = listOf(
            "Gutes Tempo!",
            "Weiter so!",
            "Du schaffst das!",
            "Gute Form!",
            "Großartig!",
            "Bleib fokussiert!",
            "Push harder!",
            "Alles klar!",
            "Ausgezeichnet!",
            "Bleib dran!"
        )
        return phrases.random()
    }

    fun estimateCalories(
        heartRate: Int,
        elapsedTimeMinutes: Long,
        bodyWeight: Float = 70f
    ): Float {
        // Mifflin-St Jeor formula approximation for cardio
        val hrFactor = heartRate / 60f
        return (bodyWeight * 0.05f * elapsedTimeMinutes) * hrFactor
    }
}
