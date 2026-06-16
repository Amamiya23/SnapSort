package com.snapsort.app.ui.copy

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun formatAperture(value: Double?): String? {
    val aperture = value?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    return "f/${formatDecimal(aperture, digits = 1)}"
}

fun formatShutterSpeed(seconds: Double?): String? {
    val value = seconds?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    if (value < 1.0) {
        val reciprocal = 1.0 / value
        val roundedReciprocal = reciprocal.roundToInt()
        if (roundedReciprocal >= 2 && abs(reciprocal - roundedReciprocal) <= roundedReciprocal * 0.02) {
            return "1/$roundedReciprocal"
        }
    }

    val digits = if (value < 10.0) 1 else 0
    return "${formatDecimal(value, digits)}s"
}

fun formatIso(value: Int?): String? {
    val iso = value?.takeIf { it > 0 } ?: return null
    return "ISO $iso"
}

private fun formatDecimal(value: Double, digits: Int): String {
    val formatted = "%.${digits}f".format(Locale.US, value)
    return formatted.trimEnd('0').trimEnd('.')
}
