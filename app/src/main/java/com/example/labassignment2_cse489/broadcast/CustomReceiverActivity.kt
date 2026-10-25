package com.example.labassignment2_cse489.broadcast

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.isVisible
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.databinding.ActivityCustomReceiverBinding
import com.example.labassignment2_cse489.util.applyWindowInsetsPadding
import com.example.labassignment2_cse489.util.formatTimeOfDay

/**
 * Activity 3 of the broadcast flow.
 *
 * Custom mode: creates a [CustomMessageReceiver], registers it while the activity is started,
 * broadcasts the text that Activity 2 put in the Intent and shows what the receiver gets.
 * Battery mode: registers nothing and only says so, as the brief asks.
 */
class CustomReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomReceiverBinding
    private val viewModel: CustomReceiverViewModel by viewModels()

    /** Created in onCreate (custom mode only); registered from onStart until onStop. */
    private var receiver: CustomMessageReceiver? = null

    /** The text from Activity 2 (custom mode only). */
    private var message: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCustomReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        // Up only finishes this activity, so Activity 2 reappears unchanged, with its extras.
        binding.toolbar.setNavigationOnClickListener { finish() }

        when (BroadcastMode.fromName(intent.getStringExtra(EXTRA_MODE))) {
            BroadcastMode.CUSTOM -> {
                val text = intent.getStringExtra(EXTRA_MESSAGE)
                if (text.isNullOrBlank()) {
                    Log.w(TAG, "Custom mode started without a message; closing")
                    finish()
                } else {
                    showCustomReceiver(text)
                }
            }
            BroadcastMode.BATTERY -> showNothingToDo()
            null -> {
                Log.w(TAG, "Started without a valid broadcast mode; closing")
                finish()
            }
        }
    }

    private fun showCustomReceiver(text: String) {
        message = text
        binding.toolbar.setTitle(R.string.title_custom_receiver)
        val content = binding.customReceiver
        content.root.isVisible = true
        content.receiverAction.text =
            getString(R.string.receiver_action, CustomMessageReceiver.ACTION_CUSTOM_MESSAGE)
        content.broadcastAgainButton.setOnClickListener { sendCustomBroadcast() }

        // The brief asks for the custom receiver to be created here, in Activity 3.
        receiver = CustomMessageReceiver { received -> onBroadcastReceived(received) }

        viewModel.lastReceived.observe(this) { received -> renderReceived(received) }
    }

    override fun onStart() {
        super.onStart()
        val receiver = receiver ?: return // battery mode: nothing to register

        // Android 14+ requires an export flag when registering a receiver for a non-system
        // broadcast. RECEIVER_NOT_EXPORTED means only this app can send to it.
        ContextCompat.registerReceiver(
            this,
            receiver,
            CustomMessageReceiver.intentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        renderRegistration(registered = true)

        // Why does Activity 3 send the broadcast itself? The text already reached this activity
        // inside the Intent that started it. Had Activity 2 broadcast it before starting us, the
        // broadcast would have been lost: at that moment this receiver did not exist yet, and a
        // normal (non-sticky) broadcast is never kept for receivers that register later. So we
        // register first and then broadcast the text to our own receiver.
        // The ViewModel remembers that it was sent, so a rotation does not send it again.
        if (!viewModel.initialBroadcastSent) {
            viewModel.initialBroadcastSent = true
            sendCustomBroadcast()
        }
    }

    override fun onStop() {
        // Unregister in the callback that matches onStart, so the receiver can never leak.
        receiver?.let { registered ->
            unregisterReceiver(registered)
            renderRegistration(registered = false)
        }
        super.onStop()
    }

    private fun sendCustomBroadcast() {
        val text = message ?: return
        sendBroadcast(CustomMessageReceiver.createBroadcast(this, text))
    }

    private fun onBroadcastReceived(text: String) {
        viewModel.onMessageReceived(text, System.currentTimeMillis())
        Toast.makeText(this, getString(R.string.received_toast, text), Toast.LENGTH_SHORT).show()
    }

    private fun renderReceived(received: ReceivedMessage?) {
        val content = binding.customReceiver
        if (received == null) {
            content.receivedMessage.setText(R.string.received_waiting)
            content.receivedMeta.isVisible = false
        } else {
            content.receivedMessage.text = getString(R.string.received_message, received.text)
            content.receivedMeta.isVisible = true
            content.receivedMeta.text = resources.getQuantityString(
                R.plurals.received_meta,
                received.count,
                formatTimeOfDay(received.receivedAtMillis),
                received.count,
            )
        }
    }

    private fun renderRegistration(registered: Boolean) {
        binding.customReceiver.receiverStatus.setText(
            if (registered) R.string.receiver_registered else R.string.receiver_not_registered,
        )
    }

    private fun showNothingToDo() {
        binding.toolbar.setTitle(R.string.title_battery_receiver)
        binding.batteryNothing.root.isVisible = true
    }

    private fun applyWindowInsets() {
        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applyWindowInsetsPadding(left = true, top = true, right = true)
        binding.scrollView.applyWindowInsetsPadding(left = true, right = true, bottom = true)
    }

    companion object {
        private const val TAG = "CustomReceiverActivity"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_MESSAGE = "message"

        /**
         * Explicit Intent that opens this activity in [mode]. [message] is the text typed in
         * Activity 2 (custom mode only).
         */
        fun newIntent(context: Context, mode: BroadcastMode, message: String? = null): Intent =
            Intent(context, CustomReceiverActivity::class.java)
                .putExtra(EXTRA_MODE, mode.name)
                .putExtra(EXTRA_MESSAGE, message)
    }
}
