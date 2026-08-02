# Bluetooth Device Setup Guide

## Moov Device BLE Integration

Diese Anleitung hilft dir, die App mit deinem echten Moov-Device zu verbinden.

### Status: In Development

Die aktuelle Implementierung nutzt:
- ✅ Standard BLE Heart Rate Profile (0000180d)
- ✅ GATT Service Discovery
- ✅ Characteristic Notifications
- ⏳ Moov-spezifische UUIDs (benötigen Device-spezifische Werte)

### Unterstützte Moov Devices

- **Moov HR**: Armband mit Herzfrequenzsensor
- **Moov Now**: Fußsensor für Lauftechnik-Analyse
- (Weitere Devices: Dokumentation wird ergänzt)

## Wie man Moov-Device-UUIDs herausfindet

### Methode 1: Mit Android BLE Scanner App

1. **"nRF Connect" App aus Google Play installieren**
   - Kostenlose App von Nordic Semiconductor

2. **Moov-Device mit Android pairen**
   - Settings → Bluetooth → Gerät suchen
   - Mit Gerät verbinden

3. **nRF Connect öffnen und "Scanner" tab öffnen**
   - Moov-Gerät in der Liste suchen
   - Auf Gerät klicken
   - "CONNECT" drücken

4. **Alle Services/Characteristics dokumentieren**
   - Jeder Service hat UUID (z.B. "12340000-...")
   - Jede Characteristic hat UUID (z.B. "12340001-...")
   - Screenshots machen!

5. **UUIDs kopieren und in Config eintragen**
   ```kotlin
   // In MoovDeviceConfig.kt
   object MoovHR {
       val SERVICE_UUID = UUID.fromString("YOUR_SERVICE_UUID")
       val SENSOR_DATA_UUID = UUID.fromString("YOUR_DATA_UUID")
   }
   ```

### Methode 2: Mit Bluetooth HCI Snoop Log (fortgeschritten)

1. **Developer Options aktivieren**
   - Settings → About Phone → Build Number (7x tippen)
   - Zurück → System → Developer Options

2. **"Enable Bluetooth HCI snoop log" aktivieren**
   - Settings → Developer Options → Bluetooth
   - Suche nach "Bluetooth HCI snoop log"

3. **Moov App öffnen und connecten**
   - Offizielle Moov App installieren
   - Mit Device connecten
   - Workout kurz starten, dann stoppen

4. **Logfile auslesen**
   ```bash
   adb pull /sdcard/Android/data/btsnoop_hci.log
   ```

5. **Mit Wireshark analysieren**
   - Wireshark herunterladen
   - btsnoop_hci.log öffnen
   - Nach GATT UUIDs filtern

## Aktuelle Implementierung

### BluetoothManager Features

```kotlin
// Verbindung zum Gerät
bluetoothManager.connectToDevice(device)

// Heart Rate abonnieren (Standard BLE)
// Automatisch wenn HRM Service gefunden wird

// Sensor-Daten abrufen
viewModel.heartRate.collect { hr ->
    // Nutze Heart Rate Daten
}

viewModel.sensorData.collect { data ->
    // Nutze Moov-spezifische Sensor-Daten
}
```

### Unterstützte BLE Standards

1. **Heart Rate Service (0000180d)**
   - Heart Rate Measurement (00002a37)
   - Automatisch für alle HR-Devices

2. **Moov-spezifische Services**
   - Konfigurierbar in `MoovDeviceConfig`
   - Support für Bewegungsdaten
   - Support für Beschleunigungssensor

## Nächste Schritte

1. **UUIDs bereitstellen**: Gib mir die BLE UUIDs deines Moov-Devices
2. **Config aktualisieren**: Ich trage die Werte in `MoovDeviceConfig.kt` ein
3. **Test durchführen**: Verbindung und Datentransfer testen
4. **Feedback-Algorithmus**: Anpassung auf echte Moov-Daten

## Wenn die Verbindung nicht funktioniert

### Checklist

- [ ] Bluetooth ist aktiviert
- [ ] Gerät ist mit Android gepaired
- [ ] Berechtigungen sind erteilt:
  - BLUETOOTH
  - BLUETOOTH_CONNECT
  - BLUETOOTH_SCAN
  - BODY_SENSORS
- [ ] Gerät ist in Reichweite (< 10m)
- [ ] Gerät ist nicht mit anderen Apps connected

### Debugging

```bash
# Logs anschauen
adb logcat | grep Bluetooth
adb logcat | grep GATT
```

## Weitere Informationen

- [Android BLE Documentation](https://developer.android.com/guide/topics/connectivity/bluetooth-le)
- [Bluetooth GATT Profile](https://www.bluetooth.com/specifications/specs/)
- [nRF Connect User Manual](https://infocenter.nordicsemi.com/index.jsp)

---

**Wichtig**: Wenn du UUIDs hast, können wir die App 100% mit deinem Moov-Device kompatibel machen!
