package com.example.labassignment2_cse489.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the mm:ss labels of the Audio screen. */
class TimeFormatTest {

    @Test
    fun formatMmSs_padsMinutesAndSeconds() {
        assertEquals("00:00", formatMmSs(0))
        assertEquals("00:09", formatMmSs(9_000))
        assertEquals("01:05", formatMmSs(65_000))
    }

    @Test
    fun formatMmSs_dropsPartialSeconds() {
        assertEquals("00:01", formatMmSs(1_999))
    }

    @Test
    fun formatMmSs_longTrack_keepsCountingMinutes() {
        assertEquals("75:00", formatMmSs(75 * 60_000L))
    }

    @Test
    fun formatMmSs_negative_isZero() {
        assertEquals("00:00", formatMmSs(-500))
    }
}
