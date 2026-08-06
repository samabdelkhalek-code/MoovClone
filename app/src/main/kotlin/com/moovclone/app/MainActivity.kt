package com.moovclone.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.ContentUris
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

// ── MODELS ──────────────────────────────────────────────────────────────────

enum class WorkoutMode(val label: String, val emoji: String, val repLabel: String) {
    BOXING("Boxing", "🥊", "Punches"),
    RUNNING("Running", "🏃", "Steps"),
    WALKING("Walking", "🚶", "Steps"),
    BODYWEIGHT("Gym", "💪", "Reps"),
    CYCLING("Cycling", "🚴", "Reps"),
    SWIMMING("Swimming", "🏊", "Strokes")
}

data class BleDeviceItem(val device: BluetoothDevice, val name: String, val rssi: Int)
data class MusicTrack(val id: Long, val title: String, val artist: String, val uri: Uri)

// ── BLE MANAGER ──────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    companion object {
        private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    val isScanning = MutableStateFlow(false)
    val devices    = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val connected  = MutableStateFlow<BleDeviceItem?>(null)
    val status     = MutableStateFlow("Disconnected")

    private val scanCb = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val name = try { result.device.name } catch (e: Exception) { null } ?: return
            if (name.isBlank()) return
            val item = BleDeviceItem(result.device, name, result.rssi)
            val list = devices.value.toMutableList()
            val idx  = list.indexOfFirst { it.device.address == item.device.address }
            if (idx >= 0) list[idx] = item else list.add(item)
            devices.value = list.sortedByDescending { it.rssi }
        }
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, st: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED    -> { status.value = "Connected ✓"; g.discoverServices() }
                BluetoothProfile.STATE_DISCONNECTED -> { status.value = "Disconnected"; connected.value = null; gatt = null }
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, st: Int) {
            if (st != BluetoothGatt.GATT_SUCCESS) return
            status.value = "Ready"
            g.services.forEach { svc ->
                svc.characteristics.forEach { ch ->
                    val canNotify = ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                                   ch.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
                    if (canNotify) {
                        g.setCharacteristicNotification(ch, true)
                        ch.getDescriptor(CCCD)?.let { desc ->
                            @Suppress("DEPRECATION")
                            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            g.writeDescriptor(desc)
                        }
                    }
                }
            }
        }
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            // Moov IMU data received — extend here for raw sensor processing
        }
    }

    fun startScan() {
        if (adapter?.isEnabled != true) { status.value = "Bluetooth OFF"; return }
        scanner = adapter.bluetoothLeScanner
        devices.value = emptyList()
        isScanning.value = true
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(null, settings, scanCb)
        handler.postDelayed({ stopScan() }, 15_000)
    }

    fun stopScan() { isScanning.value = false; scanner?.stopScan(scanCb) }

    fun connect(item: BleDeviceItem) {
        stopScan()
        gatt?.disconnect(); gatt?.close()
        status.value = "Connecting…"
        connected.value = item
        gatt = item.device.connectGatt(context, false, gattCb, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect(); gatt?.close(); gatt = null
        connected.value = null; status.value = "Disconnected"
    }
}

// ── MOTION ANALYZER ──────────────────────────────────────────────────────────

class MotionAnalyzer(context: Context) : SensorEventListener {

    private val sm    = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val repCount = MutableStateFlow(0)
    val cadence  = MutableStateFlow(0)

    private var mode     = WorkoutMode.BOXING
    private var lastPeak = 0L
    private var lastMag  = 0f
    private val times    = mutableListOf<Long>()

    private val threshold get() = when (mode) {
        WorkoutMode.BOXING     -> 17f
        WorkoutMode.RUNNING    -> 13f
        WorkoutMode.WALKING    -> 9f
        WorkoutMode.BODYWEIGHT -> 11f
        WorkoutMode.CYCLING    -> 9f
        WorkoutMode.SWIMMING   -> 12f
    }

    fun start(m: WorkoutMode) {
        mode = m; repCount.value = 0; cadence.value = 0; times.clear()
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME, Handler(Looper.getMainLooper()))
    }

    fun stop()  { sm.unregisterListener(this) }
    fun reset() { repCount.value = 0; cadence.value = 0; times.clear() }

    override fun onSensorChanged(e: SensorEvent) {
        val mag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toFloat() - 9.81f
        val now = System.currentTimeMillis()
        if (abs(mag) > threshold && lastMag <= threshold && now - lastPeak > 280) {
            lastPeak = now; repCount.value++
            times.add(now); times.removeAll { now - it > 60_000L }
            cadence.value = times.size
        }
        lastMag = abs(mag)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}

// ── COACH TTS ────────────────────────────────────────────────────────────────

class CoachTTS(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.getDefault(); ready = true }
        }
    }

    fun say(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun onRep(count: Int, mode: WorkoutMode) {
        val every = if (mode == WorkoutMode.RUNNING || mode == WorkoutMode.WALKING) 50 else 10
        if (count == 0 || count % every != 0) return
        say(when (mode) {
            WorkoutMode.BOXING     -> "$count punches! ${hype(count)}"
            WorkoutMode.BODYWEIGHT -> "$count reps! ${hype(count)}"
            WorkoutMode.RUNNING,
            WorkoutMode.WALKING    -> "$count steps!"
            WorkoutMode.SWIMMING   -> "$count strokes!"
            WorkoutMode.CYCLING    -> "$count! Keep pedaling!"
        })
    }

    fun onTick(sec: Int) {
        when (sec) {
            10  -> say("Let's go!")
            30  -> say("Great start! Keep your form!")
            60  -> say("One minute! You're doing great!")
            120 -> say("Two minutes! Stay strong!")
            300 -> say("Five minutes! You're unstoppable!")
            else -> if (sec > 60 && sec % 60 == 0) say("${sec / 60} minutes! Keep going!")
        }
    }

    fun onStart(mode: WorkoutMode) = say("Starting ${mode.label}! Let's go!")
    fun onStop(reps: Int, mode: WorkoutMode, secs: Int) =
        say("Workout complete! $reps ${mode.repLabel} in ${formatTime(secs)}. Amazing work!")

    private fun hype(n: Int) = when {
        n >= 100 -> "Absolutely incredible!"
        n >= 50  -> "You're on fire!"
        else     -> "Keep it up!"
    }

    fun destroy() { tts?.stop(); tts?.shutdown() }
}

// ── MUSIC PLAYER ─────────────────────────────────────────────────────────────

class MusicPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var idx = 0

    val tracks       = MutableStateFlow<List<MusicTrack>>(emptyList())
    val isPlaying    = MutableStateFlow(false)
    val currentTrack = MutableStateFlow<MusicTrack?>(null)

    fun loadTracks() {
        val list = mutableListOf<MusicTrack>()
        val uri  = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST)
        runCatching {
            context.contentResolver.query(uri, proj, "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { c ->
                val idC = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tiC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val arC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                while (c.moveToNext()) {
                    val id = c.getLong(idC)
                    list.add(MusicTrack(id, c.getString(tiC) ?: "Unknown",
                        c.getString(arC) ?: "Unknown", ContentUris.withAppendedId(uri, id)))
                }
            }
        }
        tracks.value = list
    }

    fun play(track: MusicTrack) {
        idx = tracks.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player?.stop(); player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build())
            runCatching {
                setDataSource(context, track.uri); prepare(); start()
                isPlaying.value = true; currentTrack.value = track
                setOnCompletionListener { next() }
            }.onFailure { isPlaying.value = false }
        }
    }

    fun togglePause() {
        val p = player ?: return
        if (p.isPlaying) { p.pause(); isPlaying.value = false }
        else             { p.start(); isPlaying.value = true  }
    }

    fun next() { val l = tracks.value; if (l.isEmpty()) return; idx = (idx + 1) % l.size; play(l[idx]) }
    fun prev() { val l = tracks.value; if (l.isEmpty()) return; idx = if (idx > 0) idx - 1 else l.size - 1; play(l[idx]) }
    fun release() { player?.stop(); player?.release(); player = null }
}

// ── VIEWMODEL ────────────────────────────────────────────────────────────────

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    val ble    = BleManager(app)
    val motion = MotionAnalyzer(app)
    val coach  = CoachTTS(app)
    val music  = MusicPlayer(app)

    private val _active  = MutableStateFlow(false)
    private val _seconds = MutableStateFlow(0)
    private val _mode    = MutableStateFlow(WorkoutMode.BOXING)

    val isActive = _active.asStateFlow()
    val seconds  = _seconds.asStateFlow()
    val mode     = _mode.asStateFlow()
    val reps     = motion.repCount.asStateFlow()
    val cadence  = motion.cadence.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) { music.loadTracks() }
        viewModelScope.launch {
            motion.repCount.collect { if (_active.value) coach.onRep(it, _mode.value) }
        }
    }

    fun selectMode(m: WorkoutMode) { if (!_active.value) _mode.value = m }

    fun startWorkout() {
        _active.value = true; _seconds.value = 0
        motion.start(_mode.value); coach.onStart(_mode.value)
        viewModelScope.launch {
            while (_active.value) {
                delay(1000)
                if (_active.value) { _seconds.value++; coach.onTick(_seconds.value) }
            }
        }
    }

    fun stopWorkout() {
        _active.value = false
        motion.stop()
        coach.onStop(motion.repCount.value, _mode.value, _seconds.value)
    }

    override fun onCleared() {
        ble.disconnect(); motion.stop(); coach.destroy(); music.release()
    }
}

// ── HELPERS ──────────────────────────────────────────────────────────────────

fun formatTime(sec: Int): String {
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ── COLORS ───────────────────────────────────────────────────────────────────

val BgDark   = Color(0xFF0A0A0F)
val Surf1    = Color(0xFF16161F)
val Surf2    = Color(0xFF1E1E2E)
val Accent   = Color(0xFF7C4DFF)
val Green    = Color(0xFF00E676)
val Red      = Color(0xFFFF1744)
val Cyan     = Color(0xFF00B0FF)

// ── ROOT COMPOSABLE ───────────────────────────────────────────────────────────

@Composable
fun MoovCoachApp(vm: WorkoutViewModel) {
    var tab by remember { mutableStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = BgDark) {
        Column(Modifier.fillMaxSize()) {
            TabRow(tab, containerColor = Surf1, contentColor = Accent) {
                listOf(Triple(Icons.Filled.FitnessCenter, "Workout"),
                       Triple(Icons.Filled.Bluetooth, "Device"),
                       Triple(Icons.Filled.MusicNote, "Music"))
                    .forEachIndexed { i, (icon, label) ->
                        Tab(selected = tab == i, onClick = { tab = i }) {
                            Column(Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, null, Modifier.size(20.dp))
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
            }
            when (tab) {
                0 -> WorkoutTab(vm)
                1 -> DeviceTab(vm)
                2 -> MusicTab(vm)
            }
        }
    }
}

// ── WORKOUT TAB ───────────────────────────────────────────────────────────────

@Composable
fun WorkoutTab(vm: WorkoutViewModel) {
    val active    by vm.isActive.collectAsState()
    val secs      by vm.seconds.collectAsState()
    val reps      by vm.reps.collectAsState()
    val cadence   by vm.cadence.collectAsState()
    val mode      by vm.mode.collectAsState()
    val connected by vm.ble.connected.collectAsState()
    val bleStatus by vm.ble.status.collectAsState()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        item {
            Text("MOOV COACH", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Surf1)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(),
                    Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape)
                            .background(if (connected != null) Green else Color.Gray))
                        Spacer(Modifier.width(8.dp))
                        Text(connected?.name ?: "No device",
                            color = if (connected != null) Green else Color.Gray, fontSize = 13.sp)
                    }
                    Text(bleStatus, color = Color.White.copy(.5f), fontSize = 11.sp)
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Surf1)) {
                Box(Modifier.fillMaxWidth().padding(28.dp), Alignment.Center) {
                    Text(formatTime(secs), fontSize = 60.sp, fontWeight = FontWeight.Thin,
                        color = if (active) Green else Color.White)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                StatCard(mode.repLabel, reps.toString(), Accent, Modifier.weight(1f))
                StatCard("Per min", cadence.toString(), Cyan, Modifier.weight(1f))
            }
        }

        item {
            Text("Mode", color = Color.White.copy(.5f), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutMode.entries.forEach { m ->
                    val sel = m == mode
                    Button(
                        onClick = { vm.selectMode(m) }, enabled = !active,
                        shape   = RoundedCornerShape(20.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor         = if (sel) Accent else Surf2,
                            disabledContainerColor = if (sel) Accent.copy(.7f) else Surf2),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("${m.emoji} ${m.label}", fontSize = 12.sp,
                            color = if (sel) Color.White else Color.White.copy(.55f))
                    }
                }
            }
        }

        item {
            Button(
                onClick  = { if (active) vm.stopWorkout() else vm.startWorkout() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(if (active) Red else Green)
            ) {
                Icon(if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow, null,
                    Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (active) "STOP" else "START WORKOUT",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (!active) item {
            Text("Zählung via Handy-Beschleunigungssensor • Voice Coaching aktiv\nVerbinde Moov-Device im Tab 'Device'",
                color = Color.White.copy(.3f), fontSize = 11.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Surf1)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = Color.White.copy(.55f))
        }
    }
}

// ── DEVICE TAB ────────────────────────────────────────────────────────────────

@Composable
fun DeviceTab(vm: WorkoutViewModel) {
    val devices   by vm.ble.devices.collectAsState()
    val scanning  by vm.ble.isScanning.collectAsState()
    val connected by vm.ble.connected.collectAsState()
    val status    by vm.ble.status.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        if (connected != null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFF0D3320))) {
                Row(Modifier.padding(14.dp).fillMaxWidth(),
                    Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(connected!!.name, color = Green,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(status, color = Green.copy(.7f), fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { vm.ble.disconnect() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) {
                        Text("Trennen")
                    }
                }
            }
        }

        Button(
            onClick  = { if (scanning) vm.ble.stopScan() else vm.ble.startScan() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(Accent)
        ) {
            if (scanning) {
                CircularProgressIndicator(Modifier.size(18.dp), Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Suche läuft… (Tippen zum Stoppen)")
            } else {
                Icon(Icons.Filled.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Nach Moov-Device suchen")
            }
        }

        Text("${devices.size} Geräte gefunden", color = Color.White.copy(.4f), fontSize = 12.sp)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices) { item ->
                val isMoov = item.name.contains("moov", ignoreCase = true)
                Card(Modifier.fillMaxWidth().clickable { vm.ble.connect(item) },
                    colors = CardDefaults.cardColors(if (isMoov) Color(0xFF0D1A3A) else Surf1)) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isMoov) Text("⭐ ", fontSize = 15.sp)
                                Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text(item.device.address, color = Color.White.copy(.4f), fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${item.rssi} dBm", color = Color.White.copy(.5f), fontSize = 12.sp)
                            Text("Verbinden →", color = Accent, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── MUSIC TAB ─────────────────────────────────────────────────────────────────

@Composable
fun MusicTab(vm: WorkoutViewModel) {
    val tracks  by vm.music.tracks.collectAsState()
    val playing by vm.music.isPlaying.collectAsState()
    val current by vm.music.currentTrack.collectAsState()

    Column(Modifier.fillMaxSize()) {
        current?.let { t ->
            Card(Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(Surf2),
                shape  = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("NOW PLAYING", fontSize = 10.sp, color = Accent,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(t.title, color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(t.artist, color = Color.White.copy(.65f), fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                        IconButton(onClick = { vm.music.prev() }) {
                            Icon(Icons.Filled.SkipPrevious, null,
                                tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Box(Modifier.size(60.dp).clip(CircleShape).background(Accent)
                            .clickable { vm.music.togglePause() },
                            contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        IconButton(onClick = { vm.music.next() }) {
                            Icon(Icons.Filled.SkipNext, null,
                                tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LibraryMusic, null,
                        tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Keine Musik gefunden", color = Color.Gray, fontSize = 15.sp)
                    Text("Musik auf Gerät speichern + Medienzugriff erlauben",
                        color = Color.Gray.copy(.7f), fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp))
                }
            }
        } else {
            Text("  ${tracks.size} Titel", color = Color.White.copy(.4f), fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tracks) { t ->
                    val isCur = t.id == current?.id
                    Card(Modifier.fillMaxWidth().clickable { vm.music.play(t) },
                        colors = CardDefaults.cardColors(if (isCur) Surf2 else Surf1)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(),
                            Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, color = Color.White, fontSize = 14.sp,
                                    fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal)
                                Text(t.artist, color = Color.White.copy(.55f), fontSize = 12.sp)
                            }
                            if (isCur) Icon(
                                if (playing) Icons.Filled.VolumeUp else Icons.Filled.MusicNote,
                                null, tint = Accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── MAIN ACTIVITY ─────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()
        setContent {
            val vm: WorkoutViewModel = viewModel()
            MoovCoachApp(vm)
        }
    }

    private fun requestNeededPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                @Suppress("DEPRECATION")
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        permLauncher.launch(perms.toTypedArray())
    }
}
