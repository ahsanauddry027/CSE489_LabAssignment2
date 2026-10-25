package com.example.labassignment2_cse489.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData

/**
 * LiveData that listens for the system battery broadcast only while someone is watching it.
 *
 * LiveData calls [onActive] when it gets its first *active* observer (one whose lifecycle is at
 * least STARTED) and [onInactive] when the last one stops. The receiver is registered in
 * onActive and unregistered in onInactive, so the app listens for ACTION_BATTERY_CHANGED only
 * while the battery screen is visible and the receiver can never leak.
 */
class BatteryStatusLiveData(context: Context) : LiveData<BatteryStatus>() {

    // The application context outlives every activity, so keeping it here cannot leak a screen.
    private val appContext = context.applicationContext

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Context-registered receivers run on the main thread, so setValue() is safe here.
            value = BatteryStatus.fromIntent(intent)
        }
    }

    override fun onActive() {
        // ACTION_BATTERY_CHANGED can only be received by a receiver registered in code (not in
        // the manifest). It is a protected broadcast that only the system may send, so it is
        // exempt from the Android 14 rule that registerReceiver() must pass RECEIVER_EXPORTED or
        // RECEIVER_NOT_EXPORTED.
        //
        // It is also *sticky*: the system keeps the last one, and registerReceiver() returns it
        // straight away, so the screen shows a value before the battery next changes.
        val stickyIntent: Intent? =
            appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        stickyIntent?.let { value = BatteryStatus.fromIntent(it) }
    }

    override fun onInactive() {
        appContext.unregisterReceiver(receiver)
    }
}
