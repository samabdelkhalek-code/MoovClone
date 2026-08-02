package com.moovclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moovclone.app.manager.Track
import com.moovclone.app.manager.MusicManager

@Composable
fun MusicPlayerCard(
    musicManager: MusicManager,
    isExpanded: Boolean = false,
    onExpandChange: (Boolean) -> Unit
) {
    val isPlaying by musicManager.isPlaying.collectAsState()
    val currentTitle by musicManager.currentTrackTitle.collectAsState()
    val volume by musicManager.volume.collectAsState()
    val playlist by musicManager.playlist.collectAsState()
    val currentBPM by musicManager.currentBPM.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onExpandChange(!isExpanded) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Compact View
            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎵 Musik", fontWeight = FontWeight.Bold)
                        Text(
                            if (currentTitle.isNotEmpty()) currentTitle else "Keine Musik gewählt",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { if (isPlaying) musicManager.pause() else musicManager.play() }) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause"
                            )
                        }
                        IconButton(onClick = { onExpandChange(!isExpanded) }) {
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand")
                        }
                    }
                }
            } else {
                // Expanded View
                Text("Musik-Player", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Current Track Info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            if (currentTitle.isNotEmpty()) currentTitle else "Wähle ein Lied",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("BPM: $currentBPM", fontSize = 12.sp)
                            Text(
                                if (isPlaying) "▶️ Läuft" else "⏸️ Pausiert",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playback Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { musicManager.previousTrack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Vorherig")
                    }
                    IconButton(
                        onClick = { if (isPlaying) musicManager.pause() else musicManager.play() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }
                    IconButton(
                        onClick = { musicManager.stop() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                    IconButton(
                        onClick = { musicManager.nextTrack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Nächstes")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Volume Control
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Lautstärke: ${String.format("%.0f", volume * 100)}%", fontSize = 12.sp)
                    Slider(
                        value = volume,
                        onValueChange = { musicManager.setVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playlist Display
                Text("Verfügbare Musik (${playlist.size} Titel)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (playlist.isEmpty()) {
                    Text(
                        "Keine Musik gefunden. Lege MP3-Dateien in den Music-Ordner",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(playlist) { track ->
                            MusicTrackItem(
                                track = track,
                                isCurrentTrack = currentTitle == track.title,
                                onClick = { musicManager.playTrack(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicTrackItem(
    track: Track,
    isCurrentTrack: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        track.artist,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "♩ ${track.bpm}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isCurrentTrack) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
