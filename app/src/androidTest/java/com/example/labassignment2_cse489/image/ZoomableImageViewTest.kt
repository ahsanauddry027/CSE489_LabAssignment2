package com.example.labassignment2_cse489.image

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.waitUntil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives [ZoomableImageView] with synthetic touch events, so the real ScaleGestureDetector and
 * GestureDetector run: a two-finger pinch zooms around the fingers within 1x..5x, a one-finger
 * drag pans only while zoomed and stops at the image edges, and a double tap zooms and resets.
 *
 * The view is 2000 x 2000 px with a 2000 x 2000 px picture, so at 1x (the fit scale of 0.5)
 * the picture covers the whole view. The large size keeps the finger spans above the minimum
 * span that ScaleGestureDetector needs on any screen density.
 */
@RunWith(AndroidJUnit4::class)
class ZoomableImageViewTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var view: ZoomableImageView

    @Before
    fun createView() = onMain {
        val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_LabAssignment2_CSE489)
        view = ZoomableImageView(context)
        view.setImageDrawable(GradientDrawable().apply { setSize(SIZE, SIZE) })
        val spec = View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        view.layout(0, 0, SIZE, SIZE)
    }

    @Test
    fun startsAtFit() {
        assertEquals(1f, onMain { view.zoom }, TOLERANCE)
        assertBounds(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()))
    }

    @Test
    fun pinchOut_zoomsInAroundTheFingers() {
        val focusX = 600f
        val focusY = 800f
        val imagePointUnderFingers = onMain { viewToImage(focusX, focusY) }

        pinch(focusX, focusY, startSpan = 1000f, endSpan = 1500f)

        val zoom = onMain { view.zoom }
        assertTrue("zoom should grow, was $zoom", zoom > 1.3f && zoom < 1.6f)
        // The picture point that was under the fingers is still under the fingers.
        val nowAt = onMain { imageToView(imagePointUnderFingers[0], imagePointUnderFingers[1]) }
        assertEquals(focusX, nowAt[0], PIXEL_TOLERANCE)
        assertEquals(focusY, nowAt[1], PIXEL_TOLERANCE)
    }

    @Test
    fun pinch_isLimitedTo5xAnd1x() {
        repeat(3) { pinch(SIZE / 2f, SIZE / 2f, startSpan = 1000f, endSpan = 1900f) }
        assertEquals(ZoomMath.MAX_ZOOM, onMain { view.zoom }, 0f)

        repeat(3) { pinch(SIZE / 2f, SIZE / 2f, startSpan = 1900f, endSpan = 1000f) }
        assertEquals(ZoomMath.MIN_ZOOM, onMain { view.zoom }, 0f)
        assertBounds(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()))
    }

    @Test
    fun drag_atFit_doesNotMoveThePicture() {
        drag(fromX = 1000f, toX = 1800f, y = 1000f)
        assertBounds(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()))
    }

    @Test
    fun drag_whileZoomed_pansButStopsAtTheEdge() {
        doubleTap(SIZE / 2f, SIZE / 2f)
        waitUntil { assertEquals(DOUBLE_TAP_ZOOM, onMain { view.zoom }, TOLERANCE) }
        val before = onMain { imageBounds() }

        // Drag far to the right: the picture follows until its left edge reaches the view's.
        drag(fromX = 200f, toX = 1900f, y = 1000f)
        drag(fromX = 200f, toX = 1900f, y = 1000f)

        val after = onMain { imageBounds() }
        assertEquals(0f, after.left, PIXEL_TOLERANCE)
        assertEquals(before.width(), after.width(), PIXEL_TOLERANCE)
        assertEquals(before.top, after.top, PIXEL_TOLERANCE) // a horizontal drag
    }

    @Test
    fun doubleTap_zoomsIn_andSecondDoubleTapResets() {
        doubleTap(SIZE / 2f, SIZE / 2f)
        waitUntil { assertEquals(DOUBLE_TAP_ZOOM, onMain { view.zoom }, TOLERANCE) }

        doubleTap(SIZE / 2f, SIZE / 2f)
        waitUntil { assertEquals(1f, onMain { view.zoom }, TOLERANCE) }
        assertBounds(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()))
    }

    // ---- Touch helpers ------------------------------------------------------------------------

    /** Two fingers on a horizontal line around ([focusX], [focusY]), moving from one span to another. */
    private fun pinch(focusX: Float, focusY: Float, startSpan: Float, endSpan: Float) {
        val downTime = SystemClock.uptimeMillis()
        var time = downTime
        // Finger 0 on the left of the focus, finger 1 on the right; [count] fingers are down.
        fun fingers(action: Int, span: Float, count: Int): MotionEvent {
            val xs = listOf(focusX - span / 2, focusX + span / 2)
            return touchEvent(downTime, time, action, xs.take(count).map { x -> x to focusY })
        }

        dispatch(fingers(MotionEvent.ACTION_DOWN, startSpan, 1))
        time += STEP_MS
        dispatch(fingers(MotionEvent.ACTION_POINTER_DOWN or SECOND_POINTER, startSpan, 2))
        for (step in 1..STEPS) {
            time += STEP_MS
            dispatch(fingers(MotionEvent.ACTION_MOVE, startSpan + (endSpan - startSpan) * step / STEPS, 2))
        }
        time += STEP_MS
        dispatch(fingers(MotionEvent.ACTION_POINTER_UP or SECOND_POINTER, endSpan, 2))
        time += STEP_MS
        dispatch(fingers(MotionEvent.ACTION_UP, endSpan, 1))
    }

    /** One finger moving horizontally from [fromX] to [toX]. */
    private fun drag(fromX: Float, toX: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        var time = downTime
        dispatch(touchEvent(downTime, time, MotionEvent.ACTION_DOWN, listOf(fromX to y)))
        for (step in 1..STEPS) {
            time += STEP_MS
            val x = fromX + (toX - fromX) * step / STEPS
            dispatch(touchEvent(downTime, time, MotionEvent.ACTION_MOVE, listOf(x to y)))
        }
        time += STEP_MS
        dispatch(touchEvent(downTime, time, MotionEvent.ACTION_UP, listOf(toX to y)))
    }

    /** Two quick taps; the gap between them is within GestureDetector's double-tap window. */
    private fun doubleTap(x: Float, y: Float) {
        val firstDown = SystemClock.uptimeMillis()
        val secondDown = firstDown + DOUBLE_TAP_GAP_MS
        dispatch(touchEvent(firstDown, firstDown, MotionEvent.ACTION_DOWN, listOf(x to y)))
        dispatch(touchEvent(firstDown, firstDown + STEP_MS, MotionEvent.ACTION_UP, listOf(x to y)))
        dispatch(touchEvent(secondDown, secondDown, MotionEvent.ACTION_DOWN, listOf(x to y)))
        dispatch(touchEvent(secondDown, secondDown + STEP_MS, MotionEvent.ACTION_UP, listOf(x to y)))
    }

    private fun touchEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        pointers: List<Pair<Float, Float>>,
    ): MotionEvent {
        val properties = Array(pointers.size) { index ->
            MotionEvent.PointerProperties().apply {
                id = index
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = Array(pointers.size) { index ->
            MotionEvent.PointerCoords().apply {
                x = pointers[index].first
                y = pointers[index].second
                pressure = 1f
                size = 1f
            }
        }
        return MotionEvent.obtain(
            downTime, eventTime, action, pointers.size, properties, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    private fun dispatch(event: MotionEvent) = onMain {
        view.dispatchTouchEvent(event)
        event.recycle()
    }

    // ---- Matrix helpers -----------------------------------------------------------------------

    private fun imageBounds(): RectF =
        RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()).also { view.imageMatrix.mapRect(it) }

    private fun imageToView(x: Float, y: Float): FloatArray =
        floatArrayOf(x, y).also { view.imageMatrix.mapPoints(it) }

    private fun viewToImage(x: Float, y: Float): FloatArray {
        val inverse = Matrix()
        view.imageMatrix.invert(inverse)
        return floatArrayOf(x, y).also { inverse.mapPoints(it) }
    }

    private fun assertBounds(expected: RectF) {
        val actual = onMain { imageBounds() }
        assertEquals(expected.left, actual.left, PIXEL_TOLERANCE)
        assertEquals(expected.top, actual.top, PIXEL_TOLERANCE)
        assertEquals(expected.right, actual.right, PIXEL_TOLERANCE)
        assertEquals(expected.bottom, actual.bottom, PIXEL_TOLERANCE)
    }

    /** Runs [block] on the main thread, where views must be used, and returns its result. */
    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private companion object {
        const val SIZE = 2000
        const val STEPS = 10
        const val STEP_MS = 16L
        const val DOUBLE_TAP_GAP_MS = 120L
        const val DOUBLE_TAP_ZOOM = 2.5f
        const val TOLERANCE = 0.01f
        const val PIXEL_TOLERANCE = 1f
        const val SECOND_POINTER = 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT
    }
}
