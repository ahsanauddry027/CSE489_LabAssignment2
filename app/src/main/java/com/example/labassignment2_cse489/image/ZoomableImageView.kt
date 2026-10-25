package com.example.labassignment2_cse489.image

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.BundleCompat

/**
 * An ImageView that zooms with a pinch, pans with one finger while zoomed and zooms in or
 * resets on a double tap.
 *
 * Everything is done through the image [Matrix] (scaleType = MATRIX), which maps drawable
 * pixels to view pixels:
 *  - "fit" is the matrix that shows the whole image centred, like FIT_CENTER;
 *  - [zoom] is the current scale divided by the fit scale, clamped to 1x..5x;
 *  - after every change the translation is clamped, so no empty gap opens at an edge and the
 *    image stays centred on an axis where it is smaller than the view.
 *
 * The fit is recomputed whenever the drawable or the view size changes; this matters because
 * the image is downloaded and set asynchronously, usually after the first layout.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /** Called with the new zoom factor (1 = fit) every time it changes. */
    var onZoomChanged: ((Float) -> Unit)? = null

    /** The current zoom relative to the fitted image; 1 means the whole image is visible. */
    var zoom: Float = ZoomMath.MIN_ZOOM
        private set

    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val mappedRect = RectF()
    private val point = FloatArray(2)

    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var tapUpDetected = false
    private var zoomAnimator: ValueAnimator? = null

    /** A zoom restored from saved state, applied once both a drawable and a size are known. */
    private var pendingState: ZoomState? = null

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener()).apply {
        // Only the two-finger pinch zooms; "double-tap and drag" would fight the double tap.
        isQuickScaleEnabled = false
    }
    private val gestureDetector = GestureDetector(context, GestureListener())

    /**
     * The ImageView constructor can call overridden methods (e.g. setImageDrawable) before this
     * class's own fields are initialised, so those overrides do nothing until this is true.
     */
    private var initialized = false

    init {
        scaleType = ScaleType.MATRIX
        initialized = true
    }

    private val contentWidth get() = (width - paddingLeft - paddingRight).toFloat()
    private val contentHeight get() = (height - paddingTop - paddingBottom).toFloat()

    // ---- Re-fitting when the drawable or the size changes -------------------------------------

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (initialized) refit(keep = null) // a new picture starts at "fit"
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Keep the zoom and the visible centre when an already laid-out view is resized. The
        // matrix still matches the old size, so the centre is measured in the old viewport.
        val keep = if (oldw > 0 && oldh > 0) {
            captureState(
                viewportWidth = (oldw - paddingLeft - paddingRight).toFloat(),
                viewportHeight = (oldh - paddingTop - paddingBottom).toFloat(),
            )
        } else {
            null
        }
        refit(keep)
    }

    /**
     * Resets the matrix to "fit" for the current drawable and size, then re-applies [keep] or a
     * zoom restored from saved state. Does nothing until both a drawable and a size exist.
     */
    private fun refit(keep: ZoomState?) {
        val drawable = drawable ?: return
        val dw = drawable.intrinsicWidth.toFloat()
        val dh = drawable.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f || contentWidth <= 0f || contentHeight <= 0f) return

        val fitScale = ZoomMath.fitScale(dw, dh, contentWidth, contentHeight)
        drawMatrix.setScale(fitScale, fitScale)
        drawMatrix.postTranslate((contentWidth - dw * fitScale) / 2f, (contentHeight - dh * fitScale) / 2f)
        zoom = ZoomMath.MIN_ZOOM

        val state = pendingState ?: keep
        pendingState = null
        if (state != null) restore(state) else applyMatrix()
    }

    // ---- Zooming and panning ------------------------------------------------------------------

    /**
     * Multiplies the zoom by [factor] around the view point ([focusX], [focusY]), so the image
     * point under the fingers stays under the fingers. The zoom is clamped to 1x..5x.
     */
    private fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
        val target = ZoomMath.clampZoom(zoom * factor)
        val applied = target / zoom
        drawMatrix.postScale(applied, applied, focusX - paddingLeft, focusY - paddingTop)
        zoom = target
        clampTranslation()
        applyMatrix()
    }

    private fun panBy(dx: Float, dy: Float) {
        drawMatrix.postTranslate(dx, dy)
        clampTranslation()
        applyMatrix()
    }

    /** Moves the image back so it never leaves an empty gap at an edge it could cover. */
    private fun clampTranslation() {
        val drawable = drawable ?: return
        mappedRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        drawMatrix.mapRect(mappedRect)
        drawMatrix.postTranslate(
            ZoomMath.edgeCorrection(mappedRect.left, mappedRect.right, contentWidth),
            ZoomMath.edgeCorrection(mappedRect.top, mappedRect.bottom, contentHeight),
        )
    }

    private fun applyMatrix() {
        imageMatrix = drawMatrix
        onZoomChanged?.invoke(zoom)
    }

    /** Smoothly zooms to [target] around ([focusX], [focusY]); used by the double tap. */
    private fun animateZoomTo(target: Float, focusX: Float, focusY: Float) {
        zoomAnimator?.cancel()
        zoomAnimator = ValueAnimator.ofFloat(zoom, target).apply {
            duration = ZOOM_ANIMATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                zoomBy((animator.animatedValue as Float) / zoom, focusX, focusY)
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) return super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) zoomAnimator?.cancel()

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // While zoomed (or pinching), don't let a parent such as the DrawerLayout steal the drag.
        if (zoom > ZoomMath.MIN_ZOOM || event.pointerCount > 1) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        // A plain tap still counts as a click, which accessibility services rely on.
        if (tapUpDetected) {
            tapUpDetected = false
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDetachedFromWindow() {
        zoomAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Follow the fingers (two-finger pan), then scale around the pinch focal point.
            drawMatrix.postTranslate(detector.focusX - lastFocusX, detector.focusY - lastFocusY)
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // One-finger pan only while zoomed in; at 1x the whole image is already visible.
            if (scaleDetector.isInProgress || zoom <= ZoomMath.MIN_ZOOM) return false
            panBy(-distanceX, -distanceY)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (zoom > ZoomMath.MIN_ZOOM + ZOOM_EPSILON) ZoomMath.MIN_ZOOM else DOUBLE_TAP_ZOOM
            animateZoomTo(target, e.x, e.y)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            tapUpDetected = true
            return false
        }
    }

    // ---- Saving the zoom across rotation ------------------------------------------------------

    /**
     * Zoom plus the image point at the centre of the view, as fractions (0..1) of the image
     * size, so the same area can be shown again in a view of a different size.
     */
    private data class ZoomState(val zoom: Float, val centerX: Float, val centerY: Float)

    private fun captureState(
        viewportWidth: Float = contentWidth,
        viewportHeight: Float = contentHeight,
    ): ZoomState? {
        val drawable = drawable ?: return null
        val dw = drawable.intrinsicWidth.toFloat()
        val dh = drawable.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) return null
        if (!drawMatrix.invert(inverseMatrix)) return null
        point[0] = viewportWidth / 2f
        point[1] = viewportHeight / 2f
        inverseMatrix.mapPoints(point) // view centre -> drawable coordinates
        return ZoomState(zoom, point[0] / dw, point[1] / dh)
    }

    /** Applies [state] on top of a freshly fitted matrix. */
    private fun restore(state: ZoomState) {
        val drawable = drawable ?: return
        zoomBy(state.zoom / zoom, paddingLeft + contentWidth / 2f, paddingTop + contentHeight / 2f)
        point[0] = state.centerX * drawable.intrinsicWidth
        point[1] = state.centerY * drawable.intrinsicHeight
        drawMatrix.mapPoints(point) // drawable point -> where it is in the view now
        panBy(contentWidth / 2f - point[0], contentHeight / 2f - point[1])
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState())
        (pendingState ?: captureState())?.let { state ->
            putFloat(KEY_ZOOM, state.zoom)
            putFloat(KEY_CENTER_X, state.centerX)
            putFloat(KEY_CENTER_Y, state.centerY)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(BundleCompat.getParcelable(state, KEY_SUPER_STATE, Parcelable::class.java))
        if (state.containsKey(KEY_ZOOM)) {
            pendingState = ZoomState(
                zoom = state.getFloat(KEY_ZOOM),
                centerX = state.getFloat(KEY_CENTER_X),
                centerY = state.getFloat(KEY_CENTER_Y),
            )
            refit(keep = null) // applies it now if the drawable and size are already known
        }
    }

    private companion object {
        const val DOUBLE_TAP_ZOOM = 2.5f
        const val ZOOM_EPSILON = 0.01f
        const val ZOOM_ANIMATION_MS = 250L

        const val KEY_SUPER_STATE = "super_state"
        const val KEY_ZOOM = "zoom"
        const val KEY_CENTER_X = "center_x"
        const val KEY_CENTER_Y = "center_y"
    }
}
