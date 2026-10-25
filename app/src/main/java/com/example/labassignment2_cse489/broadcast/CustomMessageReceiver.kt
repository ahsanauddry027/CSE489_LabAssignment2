package com.example.labassignment2_cse489.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.labassignment2_cse489.BuildConfig

/**
 * The receiver for this app's own custom broadcast, [ACTION_CUSTOM_MESSAGE].
 *
 * It is *context-registered*: CustomReceiverActivity (Activity 3) creates it, registers it in
 * onStart and unregisters it in onStop. It is deliberately not declared in the manifest,
 * because the brief asks for the receiver to be created in Activity 3.
 *
 * @param onMessage called on the main thread with the text carried in [EXTRA_MESSAGE].
 */
class CustomMessageReceiver(
    private val onMessage: (String) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CUSTOM_MESSAGE) return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
        Log.i(TAG, "Received via broadcast: $message")
        onMessage(message)
    }

    companion object {
        private const val TAG = "CustomMessageReceiver"

        /** Our own action. The application ID prefix keeps it unique on the device. */
        const val ACTION_CUSTOM_MESSAGE = "${BuildConfig.APPLICATION_ID}.action.CUSTOM_MESSAGE"

        /** The extra that carries the user's text. */
        const val EXTRA_MESSAGE = "${BuildConfig.APPLICATION_ID}.extra.MESSAGE"

        /** The filter the receiver is registered with: it only matches our custom action. */
        fun intentFilter(): IntentFilter = IntentFilter(ACTION_CUSTOM_MESSAGE)

        /**
         * Builds the broadcast for [message]. setPackage() restricts delivery to this app, so no
         * other app can receive the user's text.
         */
        fun createBroadcast(context: Context, message: String): Intent =
            Intent(ACTION_CUSTOM_MESSAGE)
                .setPackage(context.packageName)
                .putExtra(EXTRA_MESSAGE, message)
    }
}
