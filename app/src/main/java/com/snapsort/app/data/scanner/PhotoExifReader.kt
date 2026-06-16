package com.snapsort.app.data.scanner

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.snapsort.app.core.CaptureTimeSource
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoExifMetadata(
    val capturedAtMillis: Long,
    val captureTimeSource: CaptureTimeSource,
    val aperture: Double? = null,
    val shutterSpeedSeconds: Double? = null,
    val iso: Int? = null
) {
    val hasExposure: Boolean = aperture != null || shutterSpeedSeconds != null || iso != null
}

class PhotoExifReader(
    private val context: Context
) {
    fun read(uri: Uri, modifiedAtMillis: Long): PhotoExifMetadata {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                val parsed = dateTimeOriginal?.let { exifDateFormat.parse(it)?.time }
                val captureTimeSource = if (parsed != null) {
                    CaptureTimeSource.EXIF
                } else {
                    CaptureTimeSource.MODIFIED_TIME
                }
                PhotoExifMetadata(
                    capturedAtMillis = parsed ?: modifiedAtMillis,
                    captureTimeSource = captureTimeSource,
                    aperture = exif.getPositiveDouble(ExifInterface.TAG_F_NUMBER),
                    shutterSpeedSeconds = exif.getPositiveDouble(ExifInterface.TAG_EXPOSURE_TIME),
                    iso = exif.getIso()
                )
            } ?: fallbackMetadata(modifiedAtMillis)
        } catch (_: Exception) {
            fallbackMetadata(modifiedAtMillis)
        }
    }

    private fun fallbackMetadata(modifiedAtMillis: Long): PhotoExifMetadata {
        return PhotoExifMetadata(
            capturedAtMillis = modifiedAtMillis,
            captureTimeSource = CaptureTimeSource.MODIFIED_TIME
        )
    }

    private fun ExifInterface.getPositiveDouble(tag: String): Double? {
        val value = getAttributeDouble(tag, Double.NaN)
        return value.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun ExifInterface.getPositiveInt(tag: String): Int? {
        val value = getAttributeInt(tag, 0)
        return value.takeIf { it > 0 }
    }

    @Suppress("DEPRECATION")
    private fun ExifInterface.getIso(): Int? {
        return getPositiveInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: getPositiveInt(ExifInterface.TAG_ISO_SPEED_RATINGS)
    }

    companion object {
        private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    }
}
