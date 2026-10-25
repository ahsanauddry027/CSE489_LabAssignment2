package com.example.labassignment2_cse489.image

import kotlin.math.min

/**
 * The pure arithmetic behind [ZoomableImageView], kept free of Android types so it can be
 * unit-tested on the JVM.
 */
internal object ZoomMath {

    /** Zoom is relative to the fitted image: 1x shows the whole picture. */
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 5f

    /** Limits [zoom] to [MIN_ZOOM]..[MAX_ZOOM]. */
    fun clampZoom(zoom: Float): Float = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)

    /**
     * The scale that makes content of [contentWidth] x [contentHeight] fit entirely inside a
     * [viewWidth] x [viewHeight] view without distortion (like ScaleType.FIT_CENTER).
     */
    fun fitScale(contentWidth: Float, contentHeight: Float, viewWidth: Float, viewHeight: Float): Float =
        min(viewWidth / contentWidth, viewHeight / contentHeight)

    /**
     * How far content spanning [start]..[end] on one axis must move to sit correctly in a
     * viewport of [viewportSize]:
     *  - content smaller than the viewport is centred;
     *  - larger content is pulled back so no empty gap shows at either edge.
     */
    fun edgeCorrection(start: Float, end: Float, viewportSize: Float): Float {
        val size = end - start
        return when {
            size <= viewportSize -> (viewportSize - size) / 2f - start
            start > 0f -> -start
            end < viewportSize -> viewportSize - end
            else -> 0f
        }
    }
}
