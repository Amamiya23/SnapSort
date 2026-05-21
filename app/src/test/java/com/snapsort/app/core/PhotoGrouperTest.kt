package com.snapsort.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoGrouperTest {
    @Test
    fun burstGroupingUsesAdjacentIntervals() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", 0L),
                photo("B", 1_000L),
                photo("C", 2_000L)
            ),
            burstThresholdMillis = 1_000L,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(1, groups.size)
        assertEquals(PhotoGroupKind.BURST, groups[0].kind)
        assertEquals(listOf("A.JPG", "B.JPG", "C.JPG"), groups[0].photos.map { it.fileName })
    }

    @Test
    fun loosePhotosAreMergedUntilBurstCutsThem() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", 0L),
                photo("B", 5_000L),
                photo("C", 6_000L),
                photo("D", 12_000L)
            ),
            burstThresholdMillis = 1_000L,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(listOf(PhotoGroupKind.LOOSE, PhotoGroupKind.BURST, PhotoGroupKind.LOOSE), groups.map { it.kind })
        assertEquals(listOf("A.JPG"), groups[0].photos.map { it.fileName })
        assertEquals(listOf("B.JPG", "C.JPG"), groups[1].photos.map { it.fileName })
        assertEquals(listOf("D.JPG"), groups[2].photos.map { it.fileName })
    }

    @Test
    fun newestFirstReversesGroupsAndPhotos() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", 0L),
                photo("B", 1_000L),
                photo("C", 8_000L),
                photo("D", 9_000L)
            ),
            burstThresholdMillis = 1_000L,
            sortDirection = SortDirection.NEWEST_FIRST
        )

        assertEquals(listOf("D.JPG", "C.JPG"), groups[0].photos.map { it.fileName })
        assertEquals(listOf("B.JPG", "A.JPG"), groups[1].photos.map { it.fileName })
    }

    @Test
    fun sameTimeUsesFileNameThenUriAsStableSort() {
        val groups = groupPhotos(
            photos = listOf(
                photo("B", 1_000L, uri = "content://folder/b"),
                photo("A", 1_000L, uri = "content://folder/a")
            ),
            burstThresholdMillis = 0L,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(listOf("A.JPG", "B.JPG"), groups[0].photos.map { it.fileName })
    }

    private fun photo(
        baseName: String,
        capturedAtMillis: Long,
        uri: String = "content://folder/$baseName"
    ): ScannedPhoto {
        return ScannedPhoto(
            jpgUri = uri,
            fileName = "$baseName.JPG",
            baseName = baseName,
            extension = "jpg",
            capturedAtMillis = capturedAtMillis,
            captureTimeSource = CaptureTimeSource.EXIF,
            modifiedAtMillis = capturedAtMillis,
            rawMatch = null
        )
    }
}
