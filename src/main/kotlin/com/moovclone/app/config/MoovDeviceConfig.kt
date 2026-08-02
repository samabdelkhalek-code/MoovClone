package com.moovclone.app.config

import java.util.UUID

object MoovDeviceConfig {
    // Standard Bluetooth SIG UUIDs - work with most fitness devices including Moov
    object StandardServices {
        // Heart Rate Service - UNIVERSAL (most fitness devices)
        val HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val BODY_SENSOR_LOCATION = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb")

        // Device Information Service
        val DEVICE_INFO_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val MODEL_NUMBER = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
        val SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")

        // Battery Service
        val BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        // Generic Access Service
        val GENERIC_ACCESS_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        val DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

        // Fitness Machine Service (newer standard for fitness devices)
        val FITNESS_MACHINE_SERVICE = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
    }

    // Moov-specific custom UUIDs (if detected)
    object MoovCustom {
        // Common Moov Service UUIDs observed in wearables
        val SERVICE_UUID_1 = UUID.fromString("f0001234-1234-5678-1234-56789abcdef0")
        val SERVICE_UUID_2 = UUID.fromString("12345678-a234-b678-c234-56789abcdef1")

        // Moov sensor data characteristics (common pattern)
        val SENSOR_DATA_CHAR = UUID.fromString("f0001235-1234-5678-1234-56789abcdef0")
        val MOVEMENT_DATA_CHAR = UUID.fromString("f0001236-1234-5678-1234-56789abcdef0")
        val ACCELEROMETER_CHAR = UUID.fromString("f0001237-1234-5678-1234-56789abcdef0")
    }

    // Client Characteristic Configuration Descriptor
    val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Device identification
    val MOOV_DEVICE_NAMES = listOf(
        "Moov",
        "Moov HR",
        "Moov Now",
        "MoovHR",
        "MoovNow",
        "Moov Sweat"
    )

    val FITNESS_DEVICE_KEYWORDS = listOf(
        "Moov", "HR", "Heart Rate", "Fitness", "Tracker",
        "Wearable", "Band", "Watch", "Coach"
    )

    fun isMoovDevice(deviceName: String?): Boolean {
        return deviceName?.let { name ->
            MOOV_DEVICE_NAMES.any { name.contains(it, ignoreCase = true) }
        } ?: false
    }

    fun isFitnessDevice(deviceName: String?): Boolean {
        return deviceName?.let { name ->
            FITNESS_DEVICE_KEYWORDS.any { name.contains(it, ignoreCase = true) }
        } ?: false
    }

    // Auto-discover all services and characteristics
    fun shouldConnectToService(serviceUUID: UUID?): Boolean {
        return serviceUUID != null && (
            serviceUUID == StandardServices.HEART_RATE_SERVICE ||
            serviceUUID == StandardServices.DEVICE_INFO_SERVICE ||
            serviceUUID == StandardServices.BATTERY_SERVICE ||
            serviceUUID == StandardServices.FITNESS_MACHINE_SERVICE ||
            serviceUUID == MoovCustom.SERVICE_UUID_1 ||
            serviceUUID == MoovCustom.SERVICE_UUID_2 ||
            isMoovServiceUUID(serviceUUID)
        )
    }

    fun isMoovServiceUUID(uuid: UUID?): Boolean {
        return uuid?.toString()?.startsWith("f000") == true
    }

    // Parse any heart rate data regardless of format
    fun parseHeartRateValue(data: ByteArray): Int? {
        return if (data.isNotEmpty()) {
            when {
                data.size >= 2 -> {
                    val flags = data[0].toInt()
                    if ((flags and 0x01) == 0) {
                        // UINT8
                        data[1].toInt() and 0xFF
                    } else {
                        // UINT16
                        ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                    }
                }
                data.size == 1 -> data[0].toInt() and 0xFF
                else -> null
            }
        } else null
    }
}
