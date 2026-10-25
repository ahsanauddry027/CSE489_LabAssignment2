package com.example.labassignment2_cse489.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Remembers where the video was when its screen stopped (rotation, app in the background,
 * another drawer item), so playback can continue from there.
 *
 * The VideoView itself cannot keep this: it is destroyed with the fragment's view on rotation,
 * and it releases its MediaPlayer when the app goes to the background. The values live in the
 * [SavedStateHandle], so they even survive the process being killed.
 */
class VideoViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    /** Position in milliseconds to continue from. */
    val positionMs: Int
        get() = savedStateHandle[KEY_POSITION_MS] ?: 0

    /** Whether to start playing again as soon as the video is prepared. */
    val resumePlaying: Boolean
        get() = savedStateHandle[KEY_RESUME_PLAYING] ?: false

    /** Called when the screen stops. */
    fun savePlayback(positionMs: Int, resumePlaying: Boolean) {
        savedStateHandle[KEY_POSITION_MS] = positionMs
        savedStateHandle[KEY_RESUME_PLAYING] = resumePlaying
    }

    /** After the video ends, the next visit starts from the beginning. */
    fun onPlaybackCompleted() = savePlayback(positionMs = 0, resumePlaying = false)

    private companion object {
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_RESUME_PLAYING = "resume_playing"
    }
}
