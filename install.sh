#!/bin/bash

# Moov Coach - Automatisches Install-Script

echo "🎯 Moov Coach - App Installer"
echo "=============================="
echo ""

# Schritt 1: Check Android Studio
if ! command -v android-studio &> /dev/null; then
    echo "❌ Android Studio nicht gefunden!"
    echo "📥 Bitte installieren:"
    echo "   macOS: brew install --cask android-studio"
    echo "   Dann erneut ausführen"
    exit 1
fi

echo "✅ Android Studio gefunden"

# Schritt 2: Check Handy
echo ""
echo "📱 Verbinde dein Handy via USB..."
echo "⚠️  Aktiviere USB-Debugging:"
echo "   1. Einstellungen → Über das Telefon"
echo "   2. Build-Nummer 7x tippen"
echo "   3. Entwickler-Optionen → USB-Debugging AN"
echo ""
read -p "Bereit? (j/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Jj]$ ]]; then
    # Schritt 3: Build starten
    echo ""
    echo "🔨 Baue App..."
    echo ""

    cd "$(dirname "$0")"

    # Gradle wrapper initialisieren falls nicht vorhanden
    if [ ! -f "gradlew" ]; then
        echo "Initialisiere Gradle..."
        gradle wrapper --gradle-version 8.0 2>/dev/null || {
            echo "⚠️  Gradle konnte nicht initialisiert werden"
            echo "Öffne das Projekt stattdessen in Android Studio:"
            echo "open -a 'Android Studio' ."
            exit 1
        }
    fi

    # Build & Install
    ./gradlew clean assembleDebug -q

    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        echo "✅ App gebaut!"
        echo ""
        echo "📲 Installiere auf Handy..."
        adb install -r app/build/outputs/apk/debug/app-debug.apk

        if [ $? -eq 0 ]; then
            echo ""
            echo "🎉 ERFOLGREICH!"
            echo "✅ App wurde installiert"
            echo "✅ Öffne 'Moov Coach' auf deinem Handy"
            echo ""
            echo "Viel Spaß beim Trainieren! 🏃💪🏊🚴"
        else
            echo "❌ Installation fehlgeschlagen"
            echo "Stelle sicher, dass USB-Debugging aktiviert ist"
        fi
    else
        echo "❌ Build fehlgeschlagen"
        echo "Öffne Android Studio und probiere es dort:"
        echo "open -a 'Android Studio' ."
    fi
else
    echo "Abgebrochen."
fi
