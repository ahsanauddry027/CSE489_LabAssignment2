package com.example.labassignment2_cse489.image

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.Image
import coil3.asDrawable
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.databinding.FragmentImageScaleBinding
import kotlinx.coroutines.launch

/**
 * The "Image Scale" screen: a picture loaded from the internet that can be pinch-zoomed
 * (see [ZoomableImageView]), with the current zoom, a hint, a loading indicator and an error
 * state with Retry.
 */
class ImageScaleFragment : Fragment(R.layout.fragment_image_scale) {

    private var _binding: FragmentImageScaleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImageScaleViewModel by viewModels()

    /** The image currently set on the view, so re-collecting the state does not reset the zoom. */
    private var shownImage: Image? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageScaleBinding.bind(view)

        binding.zoomableImage.onZoomChanged = { zoom -> showZoom(zoom) }
        showZoom(binding.zoomableImage.zoom)
        binding.retryButton.setOnClickListener { viewModel.load() }

        // Collect the state only while the screen is visible: repeatOnLifecycle starts the block
        // in onStart and cancels it in onStop.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ImageUiState) {
        binding.loadingIndicator.isVisible = state is ImageUiState.Loading
        binding.errorGroup.isVisible = state is ImageUiState.Error
        val loaded = state is ImageUiState.Success
        binding.zoomLabel.isVisible = loaded
        binding.zoomHint.isVisible = loaded

        if (state is ImageUiState.Success && state.image !== shownImage) {
            shownImage = state.image
            binding.zoomableImage.setImageDrawable(state.image.asDrawable(resources))
        }
    }

    private fun showZoom(zoom: Float) {
        binding.zoomLabel.text = getString(R.string.zoom_level, zoom)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shownImage = null
        _binding = null
    }
}
