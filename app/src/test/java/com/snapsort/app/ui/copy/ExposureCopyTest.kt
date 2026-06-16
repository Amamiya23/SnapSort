package com.snapsort.app.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExposureCopyTest {
    @Test
    fun formatApertureTrimsTrailingZeroes() {
        assertEquals("f/2.8", formatAperture(2.8))
        assertEquals("f/4", formatAperture(4.0))
    }

    @Test
    fun formatApertureOmitsInvalidValues() {
        assertNull(formatAperture(null))
        assertNull(formatAperture(0.0))
        assertNull(formatAperture(Double.NaN))
    }

    @Test
    fun formatShutterSpeedUsesReciprocalForCommonFractions() {
        assertEquals("1/500", formatShutterSpeed(0.002))
        assertEquals("1/2", formatShutterSpeed(0.5))
    }

    @Test
    fun formatShutterSpeedUsesSecondsForLongerExposures() {
        assertEquals("2s", formatShutterSpeed(2.0))
        assertEquals("1.3s", formatShutterSpeed(1.25))
    }

    @Test
    fun formatShutterSpeedOmitsInvalidValues() {
        assertNull(formatShutterSpeed(null))
        assertNull(formatShutterSpeed(0.0))
        assertNull(formatShutterSpeed(Double.NaN))
    }

    @Test
    fun formatIsoAddsPrefixAndOmitsInvalidValues() {
        assertEquals("ISO 400", formatIso(400))
        assertNull(formatIso(null))
        assertNull(formatIso(0))
    }
}
