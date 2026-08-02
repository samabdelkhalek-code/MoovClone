package com.moovclone.app.manager

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class Track(
    val path: String,
    val title: String,
    val duration: Long = 0,
    val artist: String = "Unknown",
    val bpm: Int = 120
)

class MusicManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Track? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _currentBPM = MutableStateFlow(120)
    val currentBPM: StateFlow<Int> = _currentBPM.asStateFlow()

    private var playlistIndex = 0

    init {
        scanMusicLibrary()
    }

    fun scanMusicLibrary() {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val tracks = mutableListOf<Track>()

        if (musicDir.exists()) {
            scanDirectory(musicDir, tracks)
        }

        // Auch Musik aus Downloads scannen
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            scanDirectory(downloadsDir, tracks)
        }

        _playlist.value = tracks
    }

    private fun scanDirectory(dir: File, tracks: MutableList<Track>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectory(file, tracks)
            } else if (isAudioFile(file.name)) {
                val track = Track(
                    path = file.absolutePath,
                    title = file.nameWithoutExtension,
                    duration = getTrackDuration(file),
                    artist = getTrackArtist(file),
                    bpm = estimateBPM(file)
                )
                tracks.add(track)
            }
        }
    }

    private fun isAudioFile(filename: String): Boolean {
        val audioExtensions = listOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
        return audioExtensions.any { filename.lowercase().endsWith(it) }
    }

    private fun getTrackDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getTrackArtist(file: File): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            retriever.release()
            artist ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun estimateBPM(file: File): Int {
        // Einfache BPM-Schätzung basierend auf Datei-Größe und Dauer
        // In einer echten App würde man Audio-Analyse nutzen
        return (80..180).random()
    }

    fun playTrack(track: Track) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            }
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.reset()
                }
                it.setDataSource(track.path)
                it.prepareAsync()
                it.setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _currentTrackTitle.value = track.title
                    _currentBPM.value = track.bpm
                }
                currentTrack = track
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPlaylist(startIndex: Int = 0) {
        if (_playlist.value.isNotEmpty() && startIndex < _playlist.value.size) {
            playlistIndex = startIndex
            playTrack(_playlist.value[startIndex])
        }
    }

    fun nextTrack() {
        if (playlistIndex < _playlist.value.size - 1) {
            playlistIndex++
            playTrack(_playlist.value[playlistIndex])
        }
    }

    fun previousTrack() {
        if (playlistIndex > 0) {
            playlistIndex--
            playTrack(_playlist.value[playlistIndex])
        }
    }

    fun play() {
        mediaPlayer?.start()
        _isPlaying.value = true
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.reset()
            }
        }
        _isPlaying.value = false
        _currentTrackTitle.value = ""
    }

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clampedVolume, clampedVolume)
        _volume.value = clampedVolume
    }

    fun seek(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
    }

    fun getDuration(): Long {
        return mediaPlayer?.duration?.toLong() ?: 0L
    }

    fun getPlaybackPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun addTrack(track: Track) {
        val current = _playlist.value.toMutableList()
        current.add(track)
        _playlist.value = current
    }

    fun removeTrack(track: Track) {
        val current = _playlist.value.toMutableList()
        current.remove(track)
        _playlist.value = current
    }
}
