package com.example.labassignment2_cse489.util

import android.content.Context
import android.text.format.DateFormat
import java.util.Locale

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

/**
 * Formats a playback position or duration as mm:ss, e.g. 65 000 ms -> "01:05".
 * Negative values count as zero; durations of an hour or more keep counting minutes ("75:00").
 */
fun formatMmSs(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / MILLIS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}

/**
 * Formats a wall-clock time including seconds, following the user's 12/24-hour setting
 * (for example "9:41:07 PM" or "21:41:07"). Seconds make live updates easy to see.
 */
fun Context.formatTimeOfDay(millis: Long): String {
    val skeleton = if (DateFormat.is24HourFormat(this)) "Hms" else "hms"
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
    return DateFormat.format(pattern, millis).toString()
}
