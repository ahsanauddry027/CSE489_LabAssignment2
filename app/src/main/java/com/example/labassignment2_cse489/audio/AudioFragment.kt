package com.example.labassignment2_cse489.audio

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.databinding.FragmentAudioBinding
import com.example.labassignment2_cse489.util.formatMmSs
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

/**
 * The "Audio" screen: plays a bundled MP3 with Play/Pause, Stop, a seekable slider and
 * elapsed/total time. The MediaPlayer itself lives in [AudioPlayerViewModel].
 */
class AudioFragment : Fragment(R.layout.fragment_audio) {

    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AudioPlayerViewModel by viewModels()

    /** True while a finger drags the slider, so progress updates don't fight the finger. */
    private var isUserSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAudioBinding.bind(view)

        binding.playPauseButton.setOnClickListener { viewModel.togglePlayPause() }
        binding.stopButton.setOnClickListener { viewModel.stop() }
        setUpSeekSlider()

        // Collect the state only while the screen is visible (from onStart until onStop).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the screen (another drawer item, Home, ...) pauses the music, so it never plays
        // on behind the video. NavigationUI keeps this screen's ViewModel (and its player) alive
        // while another drawer item is shown, so without this the audio would keep going.
        // A rotation is not "leaving": playback continues through it.
        if (!requireActivity().isChangingConfigurations) viewModel.pause()
    }

    private fun setUpSeekSlider() {
        val slider = binding.seekSlider
        slider.setLabelFormatter { value -> formatMmSs(value.toLong()) }
        slider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            binding.elapsedTime.text = formatMmSs(value.toLong())
            // Keyboard and accessibility changes have no touch phase, so seek right away.
            if (!isUserSeeking) viewModel.seekTo(value.toInt())
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isUserSeeking = false
                viewModel.seekTo(slider.value.toInt())
            }
        })
    }

    private fun render(state: AudioUiState) {
        binding.playPauseButton.isEnabled = state.isPrepared
        binding.playPauseButton.setText(if (state.isPlaying) R.string.audio_pause else R.string.audio_play)
        binding.playPauseButton.setIconResource(
            if (state.isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24,
        )
        binding.stopButton.isEnabled = state.isPrepared && (state.isPlaying || state.positionMs > 0)

        // A Slider needs valueTo > valueFrom and a value inside that range.
        val durationMs = state.durationMs.coerceAtLeast(1)
        binding.seekSlider.isEnabled = state.isPrepared
        binding.seekSlider.valueTo = durationMs.toFloat()
        if (!isUserSeeking) {
            binding.seekSlider.value = state.positionMs.coerceIn(0, durationMs).toFloat()
            binding.elapsedTime.text = formatMmSs(state.positionMs.toLong())
        }
        binding.totalTime.text = formatMmSs(state.durationMs.toLong())

        binding.audioStatus.setText(
            when {
                state.hasError -> R.string.audio_error
                !state.isPrepared -> R.string.audio_preparing
                state.isPlaying -> R.string.audio_playing
                state.positionMs > 0 -> R.string.audio_paused
                else -> R.string.audio_ready
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
