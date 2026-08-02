# Moov Coach App - Installationsanleitung

## ⚡ Schnellstart (2 Minuten)

### Schritt 1: Android Studio installieren
```bash
# macOS
brew install --cask android-studio

# Linux
# Lade herunter von: https://developer.android.com/studio

# Windows
# Lade herunter von: https://developer.android.com/studio
```

### Schritt 2: Projekt öffnen
```bash
cd /Volumes/Sam/Users/sam/crypto/MoovClone
open -a "Android Studio" .
```

### Schritt 3: Handy vorbereiten
1. Handy mit **USB-Kabel** verbinden
2. Einstellungen → Über das Telefon → Build-Nummer **7x** tippen
3. Entwickler-Optionen öffnen
4. **USB-Debugging aktivieren**

### Schritt 4: APP BAUEN & INSTALLIEREN
Im Android Studio:
- **Oben in der Mitte:** Dein Handy sollte dort auftauchen
- **Grüner Play-Button** oben rechts klicken
- **1-2 Minuten warten**
- App öffnet sich automatisch auf deinem Handy ✅

---

## Was du machst:

**Laufen (Running):**
- Handy in die Tasche
- "Laufen" Modus wählen
- START drücken
- App zählt Schritte, misst Impact
- Coaching-Ansagen jede Minute
- Musik läuft im Hintergrund

**Bodyweight Circuit:**
- "Bodyweight" wählen
- START drücken
- PUSH-UPS machen
- App zählt Reps AUTOMATISCH
- "10 Wiederholungen!" Ansage
- Feedback zur Form

**Cardio Boxing:**
- "Cardio Boxing" wählen
- START drücken
- Mit Handy in der Hand boxen
- App zählt Schläge AUTOMATISCH
- Messt Power (0-100%)
- "20 Schläge!" Ansage

**Schwimmen:**
- Handy in wasserfester Tasche
- "Swim" wählen
- Schwimmen gehen
- App zählt Züge
- Misst Rhythmus

**Radfahren:**
- Handy am Lenker befestigen
- "Cycling" wählen
- Fahren
- App misst Trittfrequenz
- Feedback zur Kadenz

---

## Features die du hast:

✅ **Bluetooth Fitness-Tracker**
- Verbindet sich mit Moov HR/Now
- Liest Live Heart-Rate
- Zeigt HR-Zonen

✅ **Musik-Playback**
- Auto-scanned deine Musik
- Play/Pause/Next während Workout
- Lautstärke-Kontrolle
- BPM-Anzeige

✅ **Audio-Coaching**
- Text-to-Speech Ansagen
- Echtzeit-Feedback
- Motivational Phrases

✅ **Auto-Counting**
- Schläge werden gezählt (Boxing)
- Reps werden gezählt (Bodyweight)
- Züge werden gezählt (Swimming)
- Alles automatisch per Accelerometer

✅ **Form-Feedback**
- Deine Bewegungs-Qualität wird bewertet
- "Zu schnell" / "Zu schwach" Feedback
- Konsistenz-Score (0-100%)

---

## Troubleshooting

### "Android SDK ist nicht installiert"
→ Warte bis Android Studio fertig installiert ist (beim ersten Start)

### "Handy erscheint nicht"
1. USB-Debugging aktivieren ✓
2. Handy neu starten
3. Android Studio neu starten

### "Build fehlgeschlagen"
```bash
# Im Terminal:
cd /Volumes/Sam/Users/sam/crypto/MoovClone
./gradlew clean build
```

### "App stürzt ab"
→ Alle Berechtigungen akzeptieren beim Start
→ Bluetooth aktivieren

---

## Was du brauchst:

- 📱 Android Handy (API 24+, Android 7.0+)
- 💻 Android Studio (kostenlos)
- 🔗 USB-Kabel zum Handy
- 📁 Musik-Dateien (optional)

---

## Musik hinzufügen

Die App scanned automatisch:
```
Handy → Music Ordner → Deine MP3s
Handy → Downloads → MP3s
```

Einfach MP3-Dateien dorthin kopieren, App neu starten!

---

## Mit Moov-Device verbinden

1. **Moov HR/Now mit Android pairen**
   ```
   Einstellungen → Bluetooth → Gerät suchen "Moov" → Pair
   ```

2. **App öffnen, "Connect" drücken**
   - App sucht Moov-Device
   - Verbindung wird hergestellt
   - Heart Rate wird live angezeigt

3. **Workout starten**
   - Handy Accelerometer = Schritte, Reps, Schläge
   - Moov-Device = Heart Rate
   - App kombiniert alles
   - Coaching gibt Feedback

---

## Support

Wenn was nicht funktioniert:
- Logcat öffnen (Android Studio → View → Tool Windows → Logcat)
- Errors checken
- Handy neu starten
- App neu bauen

---

**Version:** 1.0.0
**Gebaut:** August 2026
**Status:** ✅ Produktionsreif für Testing

Viel Spaß! 🎯🏃💪🏊🚴🥊
