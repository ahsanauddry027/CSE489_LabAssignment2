package com.example.labassignment2_cse489.video

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.databinding.FragmentVideoBinding

/**
 * The "Video" screen: plays a video bundled in res/raw with a VideoView (which wraps
 * android.media.MediaPlayer) and the standard MediaController (play/pause, seek bar, times).
 *
 * When the screen goes away it pauses and remembers the position in [VideoViewModel]. After a
 * rotation it keeps playing from there; after leaving the screen it waits, paused, at that point.
 */
class VideoFragment : Fragment(R.layout.fragment_video) {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoViewModel by viewModels()

    private var mediaController: MediaController? = null

    /** The drawer of the host activity, if any (see [hideControlsWhileDrawerMoves]). */
    private var drawerLayout: DrawerLayout? = null

    /**
     * True from onPrepared until the screen stops. In the background the VideoView releases its
     * MediaPlayer, and on return it prepares the video again, calling onPrepared once more.
     */
    private var isPrepared = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVideoBinding.bind(view)

        val controller = MediaController(requireContext())
        mediaController = controller

        binding.videoView.apply {
            // VideoView anchors the controller to its parent (the black frame), so the
            // controls appear along the bottom edge of the video area.
            setMediaController(controller)
            setOnPreparedListener { onVideoPrepared() }
            setOnErrorListener { _, what, extra -> onVideoError(what, extra) }
            setOnCompletionListener { onVideoCompleted() }
            setVideoURI("android.resource://${requireContext().packageName}/${R.raw.sample_video}".toUri())
        }

        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        drawerLayout?.addDrawerListener(hideControlsWhileDrawerMoves)
    }

    override fun onStart() {
        super.onStart()
        // Back from the background the video is being prepared again.
        if (!isPrepared) showLoading()
    }

    override fun onPause() {
        super.onPause()
        // The screen is about to stop (rotation, Home, another drawer item): pause and remember
        // the position. This happens in onPause rather than onStop because on recent Android
        // versions the VideoView's surface, and with it the MediaPlayer, is already released
        // by the time onStop runs, so the position would read as 0.
        val videoView = binding.videoView
        if (isPrepared) {
            // Keep playing through a rotation; stay paused when the user really left the screen.
            viewModel.savePlayback(
                positionMs = videoView.currentPosition,
                resumePlaying = videoView.isPlaying && requireActivity().isChangingConfigurations,
            )
        }
        videoView.pause()
    }

    override fun onStop() {
        super.onStop()
        // The VideoView releases its player when its surface goes away; it prepares again (and
        // calls onPrepared again) when the screen comes back.
        isPrepared = false
    }

    override fun onDestroyView() {
        drawerLayout?.removeDrawerListener(hideControlsWhileDrawerMoves)
        drawerLayout = null
        // The controller is shown in its own window; hide it so that window cannot leak.
        mediaController?.hide()
        mediaController = null
        binding.videoView.stopPlayback() // releases the MediaPlayer
        super.onDestroyView()
        _binding = null
    }

    private fun onVideoPrepared() {
        isPrepared = true
        binding.videoLoading.isVisible = false
        binding.videoView.seekTo(viewModel.positionMs)
        if (viewModel.resumePlaying) {
            binding.videoView.start()
        } else {
            // Briefly show the controls, so the play button is easy to find.
            showControlsBriefly()
        }
    }

    private fun onVideoCompleted() {
        viewModel.onPlaybackCompleted()
        // VideoView hides the controls at the end; show them again so replay is one tap away.
        showControlsBriefly()
    }

    /**
     * Shows the controls for MediaController's default few seconds. They are not pinned with
     * show(0): the controller is a separate, focusable window, and while it is up the rest of
     * the screen (toolbar, hamburger) cannot get keyboard focus. Tapping the video shows them
     * again, and with TalkBack on MediaController ignores the timeout and keeps them visible.
     */
    private fun showControlsBriefly() {
        mediaController?.show()
    }

    /** Shows a friendly message instead of the system "Can't play this video" dialog. */
    private fun onVideoError(what: Int, extra: Int): Boolean {
        Log.e(TAG, "VideoView error: what=$what extra=$extra")
        binding.videoLoading.isVisible = false
        binding.videoError.isVisible = true
        return true // true = handled, so the system dialog is not shown
    }

    private fun showLoading() {
        binding.videoError.isVisible = false
        binding.videoLoading.isVisible = true
    }

    /**
     * MediaController draws in its own window above the activity, so an opening drawer would
     * slide *under* the controls. Hide them as soon as the drawer starts to move.
     */
    private val hideControlsWhileDrawerMoves = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            if (newState != DrawerLayout.STATE_IDLE) mediaController?.hide()
        }
    }

    private companion object {
        const val TAG = "VideoFragment"
    }
}
