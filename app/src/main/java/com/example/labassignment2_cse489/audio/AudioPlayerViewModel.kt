package com.example.labassignment2_cse489.audio

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.labassignment2_cse489.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Everything the Audio screen shows. */
data class AudioUiState(
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val hasError: Boolean = false,
)

/**
 * Owns the [MediaPlayer] of the Audio screen.
 *
 * The player lives here, not in the fragment, so playback carries on through a rotation: the
 * fragment and its views are recreated, but this ViewModel and its player stay. The player is
 * released in [onCleared], when the screen is gone for good.
 *
 * MediaPlayer is a state machine; calling a method in the wrong state throws. This class moves
 * through it like this:
 * ```
 * Idle --setDataSource--> Initialized --prepareAsync--> Preparing --(onPrepared)--> Prepared
 * Prepared / Paused / PlaybackCompleted --start--> Started --pause--> Paused
 * Started / Paused --stop--> Stopped --prepareAsync--> Preparing --> Prepared ...
 * Started --(end of track)--> PlaybackCompleted
 * any state --release--> End
 * ```
 */
class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    private val player = MediaPlayer()
    private var progressJob: Job? = null

    init {
        // Idle -> Initialized. Audio attributes must be set before preparing.
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        application.resources.openRawResourceFd(R.raw.sample_audio).use { file ->
            player.setDataSource(file)
        }
        player.setOnPreparedListener { onPrepared() }
        player.setOnCompletionListener { onCompleted() }
        player.setOnErrorListener { _, what, extra -> onError(what, extra) }
        // Initialized -> Preparing. prepareAsync() returns at once, so the UI never waits; the
        // player calls onPrepared (on the main thread) when it is ready to play.
        player.prepareAsync()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun play() {
        if (!_uiState.value.isPrepared || _uiState.value.isPlaying) return
        player.start()
        _uiState.update { it.copy(isPlaying = true) }
        startProgressUpdates()
    }

    fun pause() {
        if (!_uiState.value.isPlaying) return
        player.pause()
        stopProgressUpdates()
        _uiState.update { it.copy(isPlaying = false, positionMs = player.currentPosition) }
    }

    /** Stops and rewinds. A stopped MediaPlayer must be prepared again before it can play. */
    fun stop() {
        if (!_uiState.value.isPrepared) return
        player.stop()
        stopProgressUpdates()
        _uiState.update { it.copy(isPrepared = false, isPlaying = false, positionMs = 0) }
        player.prepareAsync()
    }

    /** Moves playback to [positionMs] (from the seek slider). */
    fun seekTo(positionMs: Int) {
        if (!_uiState.value.isPrepared) return
        player.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    private fun onPrepared() {
        _uiState.update { it.copy(isPrepared = true, durationMs = player.duration, hasError = false) }
    }

    /** End of the track: back to the start, ready to play again. */
    private fun onCompleted() {
        stopProgressUpdates()
        player.seekTo(0)
        _uiState.update { it.copy(isPlaying = false, positionMs = 0) }
    }

    private fun onError(what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        stopProgressUpdates()
        _uiState.update { it.copy(isPrepared = false, isPlaying = false, hasError = true) }
        return true // handled, so onCompletion is not called as well
    }

    /** While playing, publish the position a few times per second for the slider and timer. */
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(positionMs = player.currentPosition) }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    /** Called once the screen is gone for good: free the decoder and audio resources. */
    override fun onCleared() {
        stopProgressUpdates()
        player.release() // any state -> End
        super.onCleared()
    }

    private companion object {
        const val TAG = "AudioPlayerViewModel"
        const val PROGRESS_INTERVAL_MS = 200L
    }
}
