# Moov Coach - AI-Powered Fitness Coaching App

Ein Android-Fitness-Coaching-App mit Echtzeit-Audio-Feedback, Bluetooth-Geräte-Integration, Musikwiedergabe und automatischem Schrittzähler-Tracking.

## Features

### 🎯 Workout Tracking
- **Echtzeit-Schrittzähler**: Nutzt den Gerätesensor für genaues Step-Counting
- **Cadence-Tracking**: Misst Schritte pro Minute für optimales Trainingstempo
- **Distanz & Kalorien**: Berechnet zurückgelegte Strecke und verbrauchte Kalorien
- **Timer**: Zeigt verstrichene Trainingszeit im Format HH:MM:SS

### 🎵 Audio Coaching
- **Text-to-Speech Ansagen**: Automatische Ansagen auf Basis der Trainingsmetriken
- **Echtzeit-Feedback**: Personalisierte Tipps zur Verbesserung der Trainingstechnik
- **Musik-Integration**: Musikwiedergabe mit Lautstärkeregelung während des Trainings

### 📱 Bluetooth Integration
- **Geräte-Kopplung**: Verbindung zu Fitness-Trackern und Bluetooth-Geräten
- **Realtime-Daten**: Synchronisierung von Sensordaten vom Bluetooth-Gerät
- **Geräte-Verwaltung**: Einfache Verbindungs- und Trennungsverwaltung

### 🎨 Modernes UI
- **Material Design 3**: Modernes und intuitives Benutzerinterface
- **Jetpack Compose**: Deklarative UI-Komponenten für schnelle Entwicklung
- **Dark Mode Support**: Automatische Anpassung an Systemeinstellungen

## Anforderungen

- Android API 24+ (Android 7.0+)
- Kotlin 1.9+
- Android Studio Hedgehog (2023.1.1+)
- Gradle 8.0+

## Installation & Setup

### 1. Repository klonen
```bash
cd /path/to/project
git clone <repository-url>
cd MoovClone
```

### 2. Projekt öffnen
```bash
# Mit Android Studio öffnen
open -a "Android Studio" .
```

### 3. Dependencies installieren
Gradle wird automatisch alle Dependencies beim ersten Build installieren.

### 4. Berechtigungen einrichten
Die App benötigt folgende Berechtigungen:
- BLUETOOTH: Für Verbindung zu Bluetooth-Geräten
- BLUETOOTH_ADMIN: Für Geräteverwaltung
- BLUETOOTH_SCAN: Für Geräteerkennung (Android 12+)
- BLUETOOTH_CONNECT: Für Geräteverbindung (Android 12+)
- BODY_SENSORS: Für Schrittzähler-Sensor

Diese werden beim ersten Start der App angefordert.

### 5. Bauen & Ausführen
```bash
# Mit Gradle (CLI)
./gradlew assembleDebug

# Auf Emulator/Gerät installieren
./gradlew installDebug
```

## Architektur

Die App folgt einer MVVM-Architektur mit drei Hauptschichten:

1. **Manager**: Verwalten externe Systeme (Bluetooth, Audio, Sensoren)
2. **ViewModel**: Business-Logik und State-Management mit Kotlin Flow
3. **UI (Compose)**: Reactive UI-Komponenten mit Material Design 3

## Verwendung

### Workout starten
1. App öffnen
2. (Optional) Bluetooth-Gerät verbinden über "Connect"-Button
3. "START" Button drücken
4. App gibt Ansage "Workout started. Let's go!"
5. Während des Trainings erhält man automatisches Audio-Feedback

### Musik einschalten
1. In der Music-Card auf Play/Pause-Button klicken
2. Lautstärke über Volume-Icon anpassen

### Bluetooth-Gerät verbinden
1. Zuerst Gerät im Android-System mit Bluetooth-Gerät pairen
2. In der App auf "Connect" klicken
3. Gerät aus Liste auswählen

## Technologie Stack

- **Kotlin**: Moderne Programmiersprache für Android
- **Jetpack Compose**: Deklaratives UI-Framework
- **Material Design 3**: Google's Design-System
- **Coroutines & Flow**: Asynchrone Programmierung und Reactive State
- **Android Sensors API**: Hardware-Sensorenzugriff
- **Bluetooth API**: Wireless-Kommunikation
- **TextToSpeech**: Audio-Coaching-Ansagen
- **MediaPlayer**: Musikwiedergabe

## Lizenz

MIT License
