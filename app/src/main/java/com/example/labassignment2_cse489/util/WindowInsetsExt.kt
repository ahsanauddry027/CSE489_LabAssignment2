package com.example.labassignment2_cse489.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Pads this view by the system bar and display cutout insets on the chosen sides, on top of
 * the padding it already has in XML.
 *
 * Since targetSdk 35 every app is drawn edge-to-edge: the status bar and navigation bar are
 * transparent and the window extends behind them. Each screen therefore has to move its own
 * toolbars and controls out from under the bars, in portrait and landscape (where the
 * navigation bar may sit on the left or right).
 *
 * @param includeIme also pad the bottom for the on-screen keyboard. Edge-to-edge windows are no
 *   longer resized by `adjustResize`, so screens with text fields must apply the IME inset.
 */
fun View.applyWindowInsetsPadding(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false,
    includeIme: Boolean = false,
) {
    // Remember the XML padding once, so repeated inset passes don't keep adding to it.
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        var types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        if (includeIme) types = types or WindowInsetsCompat.Type.ime()
        val insets = windowInsets.getInsets(types)
        view.updatePadding(
            left = initialLeft + if (left) insets.left else 0,
            top = initialTop + if (top) insets.top else 0,
            right = initialRight + if (right) insets.right else 0,
            bottom = initialBottom + if (bottom) insets.bottom else 0,
        )
        // Not consumed: sibling and child views still receive the same insets.
        windowInsets
    }
}
