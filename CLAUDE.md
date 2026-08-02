# Moov Coach Development Guide

## Projektübersicht

Moov Coach ist eine Android-Fitness-Coaching-App mit Bluetooth-Integration, automatischem Schrittzähler-Tracking und AI-gesteuerten Audio-Ansagen. Die App basiert auf der Moov-App und integriert:

- **Schrittzähler**: Hardware-basiertes Step-Counting via Android Sensors API
- **Echtzeit-Coaching**: Text-to-Speech-Ansagen basierend auf Trainingsmetriken
- **Musikwiedergabe**: Integrierte Musiksteuerung während des Trainings
- **Bluetooth-Geräte**: Kopplung mit Fitness-Trackern und Wearables

## Technologie Stack

- **Kotlin**: Modern, null-safe, mit Coroutines für asynchrone Ops
- **Jetpack Compose**: Deklaratives UI-Framework mit Material Design 3
- **MVVM-Architektur**: ViewModels mit StateFlow für reactive state
- **Android APIs**: Sensors, Bluetooth, MediaPlayer, TextToSpeech

## Projektstruktur

```
MoovClone/
├── app/src/main/kotlin/com/moovclone/app/
│   ├── MainActivity.kt                    # Entry Point
│   ├── manager/
│   │   ├── BluetoothManager.kt           # Bluetooth-Handling
│   │   ├── AudioCoachManager.kt          # TTS + Musikwiedergabe
│   │   └── SensorManager.kt              # Step Counter
│   ├── viewmodel/
│   │   └── WorkoutViewModel.kt           # Business Logic + State
│   └── ui/
│       ├── WorkoutScreen.kt              # Main UI (Compose)
│       └── theme/                        # Material Design 3 Theme
├── build.gradle.kts                       # Dependencies
└── README.md                              # User Guide
```

## Wichtige Komponenten

### BluetoothManager
- Verwaltet Geräte-Kopplung und -Verbindung
- Nutzt BroadcastReceiver für Geräteerkennung
- StateFlow für UI-Updates

### AudioCoachManager
- TextToSpeech für Ansagen
- MediaPlayer für Musikwiedergabe
- Automatisches Feedback basierend auf Metriken

### StepSensorManager (SensorManager)
- Nutzt TYPE_STEP_COUNTER für genaue Schritte
- Berechnet Cadence (Schritte/Minute)
- Evaluiert Bewegungsqualität via Accelerometer

### WorkoutViewModel
- Orchestriert alle Manager
- StateFlow für Workout-Status
- Timer für Zeitmessung
- Automatisiertes Feedback alle 30 Sekunden

## Workflow für Verbesserungen

### Neue Sensoren hinzufügen
1. SensorManager erweitern (TYPE_TEMPERATURE, etc.)
2. StepSensorManager.onSensorChanged() updaten
3. WorkoutViewModel.WorkoutState erweitern
4. UI-Composables anpassen

### Neue Coaching-Tipps
1. AudioCoachManager.generateFeedback() erweitern
2. Neue konditionelle Logik hinzufügen
3. Texte in strings.xml auslagern

### Bluetooth-Features
1. BluetoothManager-Methoden erweitern
2. Neue StateFlows für Geräte-Status
3. UI-Buttons in WorkoutScreen hinzufügen

## Wichtige Berechtigungen (Android 12+)

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

Diese müssen runtime angefordert werden. Die App sollte graceful degradieren, wenn nicht gewährt.

## Testing

Unit Tests: `./gradlew test`
Instrumented Tests: `./gradlew connectedAndroidTest`

## Build & Deploy

```bash
# Debug-Build
./gradlew assembleDebug

# Release-Build (braucht signingConfig)
./gradlew assembleRelease

# Auf Gerät installieren
./gradlew installDebug
```

## Bekannte Limitationen

- Step Counter funktioniert nur mit kompatiblen Android-Geräten
- Bluetooth-Reichweite ~ 10m
- TTS hängt von Systemsprache ab
- Keine persönlichen Trainingspläne (noch)

## Zukünftige Erweiterungen

1. Workout-History mit Datenbank
2. Personalisierte Trainingspläne (ML-basiert)
3. Freunde & Challenges (Multiplayer)
4. Apple Watch / Wear OS Integration
5. Fortgeschrittene Analytik
6. Custom Coaching-Prompts via AI
