package com.example.labassignment2_cse489.broadcast

/**
 * The two options of the "Select a broadcast type" spinner.
 *
 * The entries are in the same order as `R.array.broadcast_types`, so a spinner position maps to
 * a mode with [fromPosition]. Between activities the mode travels as its [name] in an Intent
 * extra and is read back with [fromName].
 */
enum class BroadcastMode {
    /** The text typed in Activity 2 is broadcast to CustomMessageReceiver in Activity 3. */
    CUSTOM,

    /** Activity 2 shows the live battery level from the system's ACTION_BATTERY_CHANGED. */
    BATTERY;

    companion object {
        /** The mode at spinner [position], or null if the position is out of range. */
        fun fromPosition(position: Int): BroadcastMode? = entries.getOrNull(position)

        /** Parses a value written with [name]; null when it is missing or unknown. */
        fun fromName(name: String?): BroadcastMode? = entries.firstOrNull { it.name == name }
    }
}
