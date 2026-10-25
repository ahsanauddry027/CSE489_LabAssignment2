package com.example.labassignment2_cse489.image

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.Image
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the Image Scale screen shows. */
sealed interface ImageUiState {
    data object Loading : ImageUiState
    data class Success(val image: Image) : ImageUiState
    data object Error : ImageUiState
}

/**
 * Downloads the picture for the Image Scale screen with Coil and exposes the result as a
 * [StateFlow].
 *
 * Because the request runs in the ViewModel, rotating the screen does not restart the download:
 * the recreated fragment simply collects the state that is already here.
 */
class ImageScaleViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ImageUiState>(ImageUiState.Loading)
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Starts (or restarts, e.g. from the Retry button) loading the image. */
    fun load() {
        loadJob?.cancel()
        _uiState.value = ImageUiState.Loading
        loadJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val request = ImageRequest.Builder(context)
                .data(IMAGE_URL)
                // Coil normally decodes images at the size of their target view. Zooming into
                // such a downsampled bitmap looks blurry, so ask for the original resolution.
                .size(Size.ORIGINAL)
                .build()
            _uiState.value = when (val result = context.imageLoader.execute(request)) {
                is SuccessResult -> ImageUiState.Success(result.image)
                is ErrorResult -> {
                    Log.w(TAG, "Could not load $IMAGE_URL", result.throwable)
                    ImageUiState.Error
                }
            }
        }
    }

    companion object {
        /** A free 2000 x 1300 photo from Lorem Picsum (picsum.photos). */
        const val IMAGE_URL = "https://picsum.photos/id/1015/2000/1300"
        private const val TAG = "ImageScaleViewModel"
    }
}
