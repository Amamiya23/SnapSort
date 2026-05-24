package com.snapsort.app.core

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun groupPhotos(
    photos: List<ScannedPhoto>,
    burstThresholdMillis: Long,
    looseGroupThresholdMillis: Long = Long.MAX_VALUE,
    sortDirection: SortDirection
): List<PhotoGroup> {
    if (photos.isEmpty()) return emptyList()

    val ascendingPhotos = photos.sortedWith(
        compareBy<ScannedPhoto> { it.capturedAtMillis }
            .thenBy { it.fileName.lowercase() }
            .thenBy { it.jpgUri }
    )

    val ascendingGroups = mutableListOf<List<ScannedPhoto>>()
    var current = mutableListOf(ascendingPhotos.first())

    for (photo in ascendingPhotos.drop(1)) {
        val previous = current.last()
        val sameBurst = photo.capturedAtMillis - previous.capturedAtMillis <= burstThresholdMillis
        if (sameBurst) {
            current.add(photo)
        } else {
            ascendingGroups.add(current)
            current = mutableListOf(photo)
        }
    }
    ascendingGroups.add(current)

    val normalizedGroups = mutableListOf<PhotoGroup>()
    var looseBuffer = mutableListOf<ScannedPhoto>()

    fun flushLoose() {
        if (looseBuffer.isNotEmpty()) {
            normalizedGroups.add(
                PhotoGroup(
                    id = "loose-${normalizedGroups.size}",
                    kind = PhotoGroupKind.LOOSE,
                    photos = looseBuffer.toList()
                )
            )
            looseBuffer = mutableListOf()
        }
    }

    fun addLoose(photo: ScannedPhoto) {
        val previous = looseBuffer.lastOrNull()
        if (previous != null && shouldSplitLooseGroup(previous, photo, looseGroupThresholdMillis)) {
            flushLoose()
        }
        looseBuffer.add(photo)
    }

    ascendingGroups.forEach { group ->
        if (group.size >= 2) {
            flushLoose()
            normalizedGroups.add(
                PhotoGroup(
                    id = "burst-${normalizedGroups.size}",
                    kind = PhotoGroupKind.BURST,
                    photos = group
                )
            )
        } else {
            addLoose(group.single())
        }
    }
    flushLoose()

    return when (sortDirection) {
        SortDirection.OLDEST_FIRST -> normalizedGroups
        SortDirection.NEWEST_FIRST -> normalizedGroups
            .asReversed()
            .map { group -> group.copy(photos = group.photos.asReversed()) }
    }
}

private fun shouldSplitLooseGroup(
    previous: ScannedPhoto,
    current: ScannedPhoto,
    looseGroupThresholdMillis: Long
): Boolean {
    if (looseGroupThresholdMillis == Long.MAX_VALUE) return false
    return looseBucketIndex(previous.capturedAtMillis, looseGroupThresholdMillis) !=
        looseBucketIndex(current.capturedAtMillis, looseGroupThresholdMillis)
}

private fun looseBucketIndex(capturedAtMillis: Long, bucketSizeMillis: Long): Long {
    val capturedAt = Instant.ofEpochMilli(capturedAtMillis).atZone(ZoneId.systemDefault())
    val dayStart = capturedAt.toLocalDate().atStartOfDay(capturedAt.zone)
    val millisSinceDayStart = ChronoUnit.MILLIS.between(dayStart, capturedAt)
    return dayStart.toInstant().toEpochMilli() + millisSinceDayStart / bucketSizeMillis
}
