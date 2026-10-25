package com.example.labassignment2_cse489.image

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the zoom limits, the fit scale and the edge clamping of ZoomableImageView. */
class ZoomMathTest {

    @Test
    fun clampZoom_keepsZoomBetween1xAnd5x() {
        assertEquals(1f, ZoomMath.clampZoom(0.4f), DELTA)
        assertEquals(2.5f, ZoomMath.clampZoom(2.5f), DELTA)
        assertEquals(5f, ZoomMath.clampZoom(9f), DELTA)
    }

    @Test
    fun fitScale_wideImage_isLimitedByWidth() {
        // 2000 x 1300 photo in a 1000 x 2000 portrait view: the width decides.
        assertEquals(0.5f, ZoomMath.fitScale(2000f, 1300f, 1000f, 2000f), DELTA)
    }

    @Test
    fun fitScale_tallImage_isLimitedByHeight() {
        assertEquals(0.25f, ZoomMath.fitScale(400f, 4000f, 1000f, 1000f), DELTA)
    }

    @Test
    fun fitScale_smallImage_isScaledUp() {
        assertEquals(2f, ZoomMath.fitScale(500f, 250f, 1000f, 1000f), DELTA)
    }

    @Test
    fun edgeCorrection_contentSmallerThanViewport_isCentred() {
        // 400 px of content at 0..400 in a 1000 px viewport moves to 300..700.
        assertEquals(300f, ZoomMath.edgeCorrection(0f, 400f, 1000f), DELTA)
    }

    @Test
    fun edgeCorrection_gapAtStart_pullsContentBack() {
        // Zoomed content 100..2100 leaves a 100 px gap on the left.
        assertEquals(-100f, ZoomMath.edgeCorrection(100f, 2100f, 1000f), DELTA)
    }

    @Test
    fun edgeCorrection_gapAtEnd_pullsContentForward() {
        // Content -1500..500 leaves a 500 px gap on the right.
        assertEquals(500f, ZoomMath.edgeCorrection(-1500f, 500f, 1000f), DELTA)
    }

    @Test
    fun edgeCorrection_viewportCovered_doesNothing() {
        assertEquals(0f, ZoomMath.edgeCorrection(-500f, 1500f, 1000f), DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
