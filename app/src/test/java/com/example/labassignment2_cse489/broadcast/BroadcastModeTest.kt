package com.example.labassignment2_cse489.broadcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for parsing the broadcast mode from a spinner position or an Intent extra. */
class BroadcastModeTest {

    @Test
    fun fromPosition_matchesSpinnerOrder() {
        // Same order as R.array.broadcast_types: Custom first, then Battery.
        assertEquals(BroadcastMode.CUSTOM, BroadcastMode.fromPosition(0))
        assertEquals(BroadcastMode.BATTERY, BroadcastMode.fromPosition(1))
    }

    @Test
    fun fromPosition_outOfRange_returnsNull() {
        assertNull(BroadcastMode.fromPosition(-1))
        assertNull(BroadcastMode.fromPosition(2))
    }

    @Test
    fun fromName_parsesWhatNameWrites() {
        // This is how the mode travels between activities in an Intent extra.
        BroadcastMode.entries.forEach { mode ->
            assertEquals(mode, BroadcastMode.fromName(mode.name))
        }
    }

    @Test
    fun fromName_missingOrUnknown_returnsNull() {
        assertNull(BroadcastMode.fromName(null))
        assertNull(BroadcastMode.fromName(""))
        assertNull(BroadcastMode.fromName("custom")) // names are case-sensitive
        assertNull(BroadcastMode.fromName("WIFI"))
    }

    @Test
    fun customBroadcast_actionAndExtra_arePrefixedWithApplicationId() {
        assertEquals(
            "com.example.labassignment2_cse489.action.CUSTOM_MESSAGE",
            CustomMessageReceiver.ACTION_CUSTOM_MESSAGE,
        )
        assertEquals(
            "com.example.labassignment2_cse489.extra.MESSAGE",
            CustomMessageReceiver.EXTRA_MESSAGE,
        )
    }
}
