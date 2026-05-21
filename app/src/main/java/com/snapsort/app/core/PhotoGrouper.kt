package com.snapsort.app.core

fun groupPhotos(
    photos: List<ScannedPhoto>,
    burstThresholdMillis: Long,
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
            looseBuffer.add(group.single())
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
