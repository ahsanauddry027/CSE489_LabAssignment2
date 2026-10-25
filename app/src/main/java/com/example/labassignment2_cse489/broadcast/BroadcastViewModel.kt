package com.example.labassignment2_cse489.broadcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Holds the spinner selection of the Broadcast Receiver screen.
 *
 * The value lives in the [SavedStateHandle], so it survives rotation (like any ViewModel data)
 * and also the process being killed while the app is in the background.
 */
class BroadcastViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    /** The broadcast type currently selected in the spinner. */
    val selectedMode: LiveData<BroadcastMode> =
        savedStateHandle.getLiveData(KEY_SELECTED_MODE, BroadcastMode.CUSTOM)

    /** The current selection, read synchronously (e.g. when Proceed is tapped). */
    val currentMode: BroadcastMode
        get() = savedStateHandle[KEY_SELECTED_MODE] ?: BroadcastMode.CUSTOM

    fun select(mode: BroadcastMode) {
        savedStateHandle[KEY_SELECTED_MODE] = mode
    }

    private companion object {
        const val KEY_SELECTED_MODE = "selected_mode"
    }
}
