package com.moovclone.app.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothManagerImpl(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    private val _sensorData = MutableStateFlow<ByteArray?>(null)
    val sensorData: StateFlow<ByteArray?> = _sensorData.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null

    // Standard BLE UUIDs for Fitness Devices
    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Moov-specific UUIDs (if available)
    private val MOOV_SERVICE_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    private val MOOV_DATA_UUID = UUID.fromString("87654321-4321-8765-4321-0fedcba98765")

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                _isConnecting.value = false
                _connectedDevice.value = gatt?.device
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                _connectedDevice.value = null
                _isConnecting.value = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                            enableNotifications(gatt, characteristic)
                        } else if (characteristic.uuid == MOOV_DATA_UUID) {
                            enableNotifications(gatt, characteristic)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            value: ByteArray?
        ) {
            value?.let {
                when (characteristic?.uuid) {
                    HEART_RATE_MEASUREMENT_UUID -> parseHeartRate(it)
                    MOOV_DATA_UUID -> _sensorData.value = it
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            value: ByteArray?,
            status: Int
        ) {
            value?.let {
                when (characteristic?.uuid) {
                    HEART_RATE_MEASUREMENT_UUID -> parseHeartRate(it)
                    MOOV_DATA_UUID -> _sensorData.value = it
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun parseHeartRate(data: ByteArray) {
        if (data.isNotEmpty()) {
            val hr = if (data[0].toInt() and 0x01 == 0) {
                data[1].toInt() and 0xFF
            } else {
                ((data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8))
            }
            _heartRate.value = hr
        }
    }

    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        updatePairedDevices()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun updatePairedDevices() {
        _pairedDevices.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bluetoothAdapter?.isDiscovering == false) {
            _isScanning.value = true
            bluetoothAdapter?.startDiscovery()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(deviceReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(deviceReceiver, filter)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
        try {
            context.unregisterReceiver(deviceReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        _isConnecting.value = true
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectedDevice.value = null
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false
}
