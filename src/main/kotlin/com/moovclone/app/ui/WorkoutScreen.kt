package com.moovclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moovclone.app.viewmodel.WorkoutViewModel

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel) {
    val workoutState by viewModel.workoutState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val stepCount by viewModel.stepCount.collectAsStateWithLifecycle()
    val cadence by viewModel.cadence.collectAsStateWithLifecycle()

    var showBluetoothSheet by remember { mutableStateOf(false) }
    var showMusicVolumeSlider by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moov Coach") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (workoutState.isRunning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.pauseWorkout() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause")
                        }
                        FloatingActionButton(
                            onClick = { viewModel.stopWorkout() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop")
                        }
                    }
                } else {
                    FloatingActionButton(
                        onClick = { viewModel.startWorkout() },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bluetooth Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bluetooth Device", fontWeight = FontWeight.Bold)
                        Button(onClick = { showBluetoothSheet = true }) {
                            Text(if (connectedDevice != null) "Connected" else "Connect")
                        }
                    }
                    if (connectedDevice != null) {
                        Text("Device: ${connectedDevice?.name}", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Elapsed Time Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatTime(workoutState.elapsedTime),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Steps",
                    value = workoutState.stepCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Cadence",
                    value = String.format("%.0f", workoutState.cadence),
                    unit = "spm",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Distance",
                    value = String.format("%.2f", workoutState.distance),
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Calories",
                    value = String.format("%.0f", workoutState.calories),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Music Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Music", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { viewModel.toggleMusic() }) {
                                Icon(
                                    if (workoutState.isPlayingMusic) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Toggle Music"
                                )
                            }
                            IconButton(onClick = { showMusicVolumeSlider = !showMusicVolumeSlider }) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "Volume")
                            }
                        }
                    }
                    if (showMusicVolumeSlider) {
                        Slider(
                            value = workoutState.musicVolume,
                            onValueChange = { viewModel.setMusicVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        Text("Volume: ${String.format("%.0f", workoutState.musicVolume * 100)}%", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showBluetoothSheet) {
        BluetoothDeviceSheet(
            devices = pairedDevices,
            onDeviceSelected = { device ->
                viewModel.connectBluetoothDevice(device.name ?: "Unknown")
                showBluetoothSheet = false
            },
            onDismiss = { showBluetoothSheet = false }
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (unit != null) {
                Text(unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BluetoothDeviceSheet(
    devices: List<Any>,
    onDeviceSelected: (Any) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismiss = { onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Available Devices", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            devices.forEach { device ->
                Button(
                    onClick = { onDeviceSelected(device) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text((device as? android.bluetooth.BluetoothDevice)?.name ?: "Unknown")
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
