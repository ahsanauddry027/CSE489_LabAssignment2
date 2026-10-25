package com.example.labassignment2_cse489.broadcast

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for the battery percentage maths and the EXTRA_STATUS mapping. */
class BatteryStatusTest {

    @Test
    fun percentOf_usualScaleOf100_returnsLevel() {
        assertEquals(42, BatteryStatus.percentOf(level = 42, scale = 100))
    }

    @Test
    fun percentOf_otherScale_isScaledTo100() {
        // Some devices report the level out of a scale other than 100.
        assertEquals(50, BatteryStatus.percentOf(level = 128, scale = 256))
        assertEquals(75, BatteryStatus.percentOf(level = 3, scale = 4))
    }

    @Test
    fun percentOf_roundsDown() {
        assertEquals(33, BatteryStatus.percentOf(level = 1, scale = 3))
    }

    @Test
    fun percentOf_emptyAndFull_returnBounds() {
        assertEquals(0, BatteryStatus.percentOf(level = 0, scale = 100))
        assertEquals(100, BatteryStatus.percentOf(level = 100, scale = 100))
    }

    @Test
    fun percentOf_zeroOrNegativeScale_returnsNull() {
        // Guards against dividing by zero when the broadcast has no usable scale.
        assertNull(BatteryStatus.percentOf(level = 50, scale = 0))
        assertNull(BatteryStatus.percentOf(level = 50, scale = -1))
    }

    @Test
    fun percentOf_missingLevel_returnsNull() {
        // getIntExtra() returns the default -1 when EXTRA_LEVEL is missing.
        assertNull(BatteryStatus.percentOf(level = -1, scale = 100))
    }

    @Test
    fun percentOf_levelAboveScale_isClampedTo100() {
        assertEquals(100, BatteryStatus.percentOf(level = 120, scale = 100))
    }

    @Test
    fun chargingStatus_mapsEveryBatteryManagerValue() {
        assertEquals(ChargingStatus.CHARGING, ChargingStatus.fromExtra(BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals(ChargingStatus.DISCHARGING, ChargingStatus.fromExtra(BatteryManager.BATTERY_STATUS_DISCHARGING))
        assertEquals(ChargingStatus.FULL, ChargingStatus.fromExtra(BatteryManager.BATTERY_STATUS_FULL))
        assertEquals(ChargingStatus.NOT_CHARGING, ChargingStatus.fromExtra(BatteryManager.BATTERY_STATUS_NOT_CHARGING))
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromExtra(BatteryManager.BATTERY_STATUS_UNKNOWN))
    }

    @Test
    fun chargingStatus_unexpectedValue_isUnknown() {
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromExtra(-1))
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromExtra(99))
    }
}
