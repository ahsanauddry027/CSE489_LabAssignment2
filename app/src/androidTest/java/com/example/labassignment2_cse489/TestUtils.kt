package com.example.labassignment2_cse489

import android.os.SystemClock
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

private const val DEFAULT_TIMEOUT_MS = 5_000L
private const val POLL_INTERVAL_MS = 100L

/**
 * Retries [assertion] until it passes or [timeoutMs] runs out.
 *
 * Espresso waits for the main thread to be idle, but not for things that happen outside the
 * app's process: a broadcast is delivered by the system a moment after sendBroadcast(), and a
 * drawer slides open frame by frame. Polling keeps such checks reliable.
 */
fun waitUntil(timeoutMs: Long = DEFAULT_TIMEOUT_MS, assertion: () -> Unit) {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    while (true) {
        try {
            assertion()
            return
        } catch (failure: Throwable) {
            if (SystemClock.uptimeMillis() > deadline) throw failure
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
    }
}

/**
 * Finishes the activities a test opened on top of MainActivity (Activity 2 and 3), so the next
 * test starts from a clean screen. MainActivity itself is closed by its ActivityScenarioRule.
 */
fun finishActivitiesAboveMain() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
            .flatMap { stage -> monitor.getActivitiesInStage(stage) }
            .filterNot { it is MainActivity }
            .forEach { it.finish() }
    }
}

/** Matches a [TextInputLayout] that currently shows [expectedError]. */
fun hasTextInputError(expectedError: String): Matcher<View> = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("TextInputLayout with error \"$expectedError\"")
    }

    override fun matchesSafely(view: View): Boolean =
        view is TextInputLayout && view.error?.toString() == expectedError
}
