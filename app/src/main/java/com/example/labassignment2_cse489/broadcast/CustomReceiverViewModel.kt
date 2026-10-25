package com.example.labassignment2_cse489.broadcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.io.Serializable

/**
 * State of Activity 3 (custom mode) that must survive rotation:
 *  - whether the first broadcast was already sent, so a rotation does not send it again;
 *  - the last message the receiver got, so it stays on screen.
 *
 * Both are kept in the [SavedStateHandle], so they even survive the process being killed while
 * the app is in the background.
 */
class CustomReceiverViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    /** The last message received, or null while waiting for the first broadcast. */
    val lastReceived: LiveData<ReceivedMessage?> = savedStateHandle.getLiveData(KEY_RECEIVED, null)

    /** True once Activity 3 has sent its first broadcast. */
    var initialBroadcastSent: Boolean
        get() = savedStateHandle[KEY_INITIAL_SENT] ?: false
        set(value) {
            savedStateHandle[KEY_INITIAL_SENT] = value
        }

    /** Records a delivery of the custom broadcast. */
    fun onMessageReceived(text: String, receivedAtMillis: Long) {
        val count = (lastReceived.value?.count ?: 0) + 1
        savedStateHandle[KEY_RECEIVED] = ReceivedMessage(text, receivedAtMillis, count)
    }

    private companion object {
        const val KEY_RECEIVED = "last_received"
        const val KEY_INITIAL_SENT = "initial_broadcast_sent"
    }
}

/**
 * One delivery of the custom broadcast.
 *
 * @property count how many broadcasts this screen has received so far.
 */
data class ReceivedMessage(
    val text: String,
    val receivedAtMillis: Long,
    val count: Int,
) : Serializable // so the SavedStateHandle can put it in a Bundle
