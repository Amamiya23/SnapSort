package com.snapsort.app.core

enum class CaptureTimeSource {
    EXIF,
    MODIFIED_TIME
}

enum class SortDirection {
    NEWEST_FIRST,
    OLDEST_FIRST
}

enum class PhotoGroupKind {
    BURST,
    LOOSE
}

data class ScannedFile(
    val uri: String,
    val fileName: String,
    val modifiedAtMillis: Long
) {
    val extension: String = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    val baseName: String = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
}

data class ScannedPhoto(
    val jpgUri: String,
    val fileName: String,
    val baseName: String,
    val extension: String,
    val capturedAtMillis: Long,
    val captureTimeSource: CaptureTimeSource,
    val modifiedAtMillis: Long,
    val rawMatch: RawMatch?
)

data class RawMatch(
    val uri: String,
    val fileName: String,
    val extension: String
)

data class PhotoGroup(
    val id: String,
    val kind: PhotoGroupKind,
    val photos: List<ScannedPhoto>
) {
    val startMillis: Long = photos.first().capturedAtMillis
    val endMillis: Long = photos.last().capturedAtMillis
}
