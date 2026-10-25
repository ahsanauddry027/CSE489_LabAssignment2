package com.example.labassignment2_cse489.broadcast

import android.content.Intent
import android.os.BatteryManager
import androidx.annotation.StringRes
import com.example.labassignment2_cse489.R

/**
 * What the battery screen shows from one ACTION_BATTERY_CHANGED broadcast.
 *
 * @property percent battery level from 0 to 100, or null if the broadcast had no usable level.
 * @property chargingStatus the decoded BatteryManager.EXTRA_STATUS value.
 * @property receivedAtMillis wall-clock time at which the app read the broadcast.
 */
data class BatteryStatus(
    val percent: Int?,
    val chargingStatus: ChargingStatus,
    val receivedAtMillis: Long,
) {
    companion object {
        /** Value returned by getIntExtra() when the battery intent lacks an extra. */
        private const val EXTRA_MISSING = -1
        private const val PERCENT_MAX = 100

        /**
         * Battery percentage = EXTRA_LEVEL * 100 / EXTRA_SCALE.
         *
         * Returns null when the level is missing (negative) or the scale is not positive, which
         * would otherwise be a division by zero. The result is clamped to 0..100.
         */
        fun percentOf(level: Int, scale: Int): Int? {
            if (level < 0 || scale <= 0) return null
            return (level * PERCENT_MAX / scale).coerceIn(0, PERCENT_MAX)
        }

        /** Reads the extras of an ACTION_BATTERY_CHANGED [intent]. */
        fun fromIntent(
            intent: Intent,
            receivedAtMillis: Long = System.currentTimeMillis(),
        ): BatteryStatus = BatteryStatus(
            percent = percentOf(
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, EXTRA_MISSING),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, EXTRA_MISSING),
            ),
            chargingStatus = ChargingStatus.fromExtra(
                intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
            ),
            receivedAtMillis = receivedAtMillis,
        )
    }
}

/** The BatteryManager.EXTRA_STATUS values, each with the label shown on screen. */
enum class ChargingStatus(@StringRes val labelRes: Int) {
    CHARGING(R.string.battery_status_charging),
    DISCHARGING(R.string.battery_status_discharging),
    FULL(R.string.battery_status_full),
    NOT_CHARGING(R.string.battery_status_not_charging),
    UNKNOWN(R.string.battery_status_unknown);

    companion object {
        /** Maps a BatteryManager.BATTERY_STATUS_* constant; anything unexpected is [UNKNOWN]. */
        fun fromExtra(status: Int): ChargingStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> NOT_CHARGING
            else -> UNKNOWN
        }
    }
}
