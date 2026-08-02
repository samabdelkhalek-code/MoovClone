package com.moovclone.app.config

import java.util.UUID

object MoovDeviceConfig {
    // Moov HR UUIDs (update with actual values when available)
    object MoovHR {
        val SERVICE_UUID = UUID.fromString("12340000-1234-5678-1234-56789abcdef0")
        val SENSOR_DATA_UUID = UUID.fromString("12340001-1234-5678-1234-56789abcdef0")
        val HEART_RATE_UUID = UUID.fromString("12340002-1234-5678-1234-56789abcdef0")
        val STEP_COUNT_UUID = UUID.fromString("12340003-1234-5678-1234-56789abcdef0")
    }

    // Moov Now UUIDs (update with actual values when available)
    object MoovNow {
        val SERVICE_UUID = UUID.fromString("56780000-5678-1234-5678-1234abcdef01")
        val SENSOR_DATA_UUID = UUID.fromString("56780001-5678-1234-5678-1234abcdef01")
        val MOVEMENT_UUID = UUID.fromString("56780002-5678-1234-5678-1234abcdef01")
    }

    // Standard BLE UUIDs for Heart Rate
    object StandardHR {
        val SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    }

    // Client Characteristic Configuration
    val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Device names to identify Moov devices
    val MOOV_DEVICE_NAMES = listOf(
        "Moov",
        "Moov HR",
        "Moov Now",
        "MoovHR",
        "MoovNow"
    )

    fun isMoovDevice(deviceName: String?): Boolean {
        return deviceName?.let { name ->
            MOOV_DEVICE_NAMES.any { name.contains(it, ignoreCase = true) }
        } ?: false
    }
}
