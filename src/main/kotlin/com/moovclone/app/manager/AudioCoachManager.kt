package com.moovclone.app.manager

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioCoachManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String) {
        tts?.let {
            if (it.isSpeaking) {
                it.stop()
            }
            _isSpeaking.value = true
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun playCoachingFeedback(steps: Int, cadence: Int, form: String) {
        val feedback = when {
            cadence < 160 -> "Cadence too low, increase pace"
            cadence > 180 -> "Great pace, maintain it"
            form == "good" -> "Excellent form, keep it up"
            else -> "Adjust your posture"
        }
        speak(feedback)
    }

    fun playMusic(musicPath: String? = null) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            }
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.reset()
                }
                if (musicPath != null) {
                    it.setDataSource(musicPath)
                    it.prepareAsync()
                } else {
                    it.setVolume(0.5f, 0.5f)
                }
                it.start()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.reset()
            }
        }
        _isPlaying.value = false
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun resumeMusic() {
        mediaPlayer?.start()
        _isPlaying.value = true
    }

    fun release() {
        tts?.shutdown()
        mediaPlayer?.release()
    }
}
