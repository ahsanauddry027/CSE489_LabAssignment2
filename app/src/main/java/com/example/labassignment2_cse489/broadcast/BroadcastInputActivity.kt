package com.example.labassignment2_cse489.broadcast

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewGroupCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.databinding.ActivityBroadcastInputBinding
import com.example.labassignment2_cse489.util.applyWindowInsetsPadding
import com.example.labassignment2_cse489.util.formatTimeOfDay

/**
 * Activity 2 of the broadcast flow. Depending on the mode chosen on the Broadcast Receiver
 * screen it shows:
 *  - CUSTOM: a text field. The typed text is passed to Activity 3 inside the Intent.
 *  - BATTERY: the live battery percentage from the system's ACTION_BATTERY_CHANGED broadcast.
 */
class BroadcastInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBroadcastInputBinding

    // Created lazily, so in custom mode no battery LiveData (and no receiver) ever exists.
    private val batteryViewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBroadcastInputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        // Up just finishes this activity, which returns to the Broadcast Receiver screen.
        binding.toolbar.setNavigationOnClickListener { finish() }

        when (BroadcastMode.fromName(intent.getStringExtra(EXTRA_MODE))) {
            BroadcastMode.CUSTOM -> showCustomInput()
            BroadcastMode.BATTERY -> showBatteryStatus()
            null -> {
                // Only BroadcastFragment starts this activity, and it always sends a mode.
                Log.w(TAG, "Started without a valid broadcast mode; closing")
                finish()
            }
        }
    }

    // ---- Custom mode --------------------------------------------------------------------------

    private fun showCustomInput() {
        binding.toolbar.setTitle(R.string.title_custom_input)
        val content = binding.customInput
        content.root.isVisible = true

        // Clear the "blank message" error as soon as the user types again.
        content.messageInput.doAfterTextChanged { content.messageLayout.error = null }
        // The keyboard's Send action does the same as the button.
        content.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
        content.sendButton.setOnClickListener { sendMessage() }
    }

    /** Opens Activity 3 with the typed text, or shows an inline error if the text is blank. */
    private fun sendMessage() {
        val content = binding.customInput
        val message = content.messageInput.text?.toString().orEmpty().trim()
        if (message.isEmpty()) {
            content.messageLayout.error = getString(R.string.error_blank_message)
            return
        }
        startActivity(CustomReceiverActivity.newIntent(this, BroadcastMode.CUSTOM, message))
    }

    // ---- Battery mode -------------------------------------------------------------------------

    private fun showBatteryStatus() {
        binding.toolbar.setTitle(R.string.title_battery_status)
        val content = binding.batteryStatus
        content.root.isVisible = true
        content.nextButton.setOnClickListener {
            startActivity(CustomReceiverActivity.newIntent(this, BroadcastMode.BATTERY))
        }

        // Observing with this activity as the LifecycleOwner makes the LiveData active (it
        // registers its receiver) in onStart and inactive (it unregisters) in onStop.
        batteryViewModel.batteryStatus.observe(this) { status -> renderBattery(status) }
    }

    private fun renderBattery(status: BatteryStatus) {
        val content = binding.batteryStatus
        val percent = status.percent
        content.batteryPercent.text = if (percent != null) {
            getString(R.string.battery_percent, percent)
        } else {
            getString(R.string.battery_percent_unknown)
        }
        content.batteryIndicator.setProgressCompat(percent ?: 0, true)
        content.batteryStatusText.text =
            getString(R.string.battery_status_label, getString(status.chargingStatus.labelRes))
        content.batteryUpdated.text =
            getString(R.string.battery_last_update, formatTimeOfDay(status.receivedAtMillis))
    }

    private fun applyWindowInsets() {
        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsPadding(left = true, top = true, right = true)
        // includeIme keeps the text field and the Send button above the on-screen keyboard.
        binding.scrollView.applyWindowInsetsPadding(
            left = true,
            right = true,
            bottom = true,
            includeIme = true,
        )
    }

    companion object {
        private const val TAG = "BroadcastInputActivity"
        private const val EXTRA_MODE = "mode"

        /** Explicit Intent that opens this activity in the given [mode]. */
        fun newIntent(context: Context, mode: BroadcastMode): Intent =
            Intent(context, BroadcastInputActivity::class.java).putExtra(EXTRA_MODE, mode.name)
    }
}
