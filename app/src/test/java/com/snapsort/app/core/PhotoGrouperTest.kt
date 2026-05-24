package com.snapsort.app.core

import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class PhotoGrouperTest {
    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

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
            looseGroupThresholdMillis = Long.MAX_VALUE,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(listOf(PhotoGroupKind.LOOSE, PhotoGroupKind.BURST, PhotoGroupKind.LOOSE), groups.map { it.kind })
        assertEquals(listOf("A.JPG"), groups[0].photos.map { it.fileName })
        assertEquals(listOf("B.JPG", "C.JPG"), groups[1].photos.map { it.fileName })
        assertEquals(listOf("D.JPG"), groups[2].photos.map { it.fileName })
    }

    @Test
    fun loosePhotosSplitByFixedLocalTimeBuckets() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", millis(2026, 5, 23, 15, 30)),
                photo("B", millis(2026, 5, 23, 15, 52)),
                photo("C", millis(2026, 5, 23, 16, 10)),
                photo("D", millis(2026, 5, 23, 17, 52))
            ),
            burstThresholdMillis = 1_000L,
            looseGroupThresholdMillis = 60 * 60 * 1_000L,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(listOf(PhotoGroupKind.LOOSE, PhotoGroupKind.LOOSE, PhotoGroupKind.LOOSE), groups.map { it.kind })
        assertEquals(listOf("A.JPG", "B.JPG"), groups[0].photos.map { it.fileName })
        assertEquals(listOf("C.JPG"), groups[1].photos.map { it.fileName })
        assertEquals(listOf("D.JPG"), groups[2].photos.map { it.fileName })
    }

    @Test
    fun loosePhotosSplitByFixedHalfHourBuckets() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", millis(2026, 5, 23, 15, 10)),
                photo("B", millis(2026, 5, 23, 15, 29)),
                photo("C", millis(2026, 5, 23, 15, 30)),
                photo("D", millis(2026, 5, 23, 15, 59))
            ),
            burstThresholdMillis = 1_000L,
            looseGroupThresholdMillis = 30 * 60 * 1_000L,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(listOf("A.JPG", "B.JPG"), groups[0].photos.map { it.fileName })
        assertEquals(listOf("C.JPG", "D.JPG"), groups[1].photos.map { it.fileName })
    }

    @Test
    fun loosePhotosDoNotSplitWhenLooseAutoSplitIsDisabled() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", millis(2026, 5, 23, 15, 30)),
                photo("B", millis(2026, 5, 23, 17, 52))
            ),
            burstThresholdMillis = 1_000L,
            looseGroupThresholdMillis = Long.MAX_VALUE,
            sortDirection = SortDirection.OLDEST_FIRST
        )

        assertEquals(1, groups.size)
        assertEquals(listOf("A.JPG", "B.JPG"), groups[0].photos.map { it.fileName })
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
    fun newestFirstKeepsGroupRangeOldestToNewest() {
        val groups = groupPhotos(
            photos = listOf(
                photo("A", millis(2026, 5, 23, 15, 30)),
                photo("B", millis(2026, 5, 23, 15, 52))
            ),
            burstThresholdMillis = 1_000L,
            looseGroupThresholdMillis = 60 * 60 * 1_000L,
            sortDirection = SortDirection.NEWEST_FIRST
        )

        assertEquals(listOf("B.JPG", "A.JPG"), groups[0].photos.map { it.fileName })
        assertEquals(millis(2026, 5, 23, 15, 30), groups[0].startMillis)
        assertEquals(millis(2026, 5, 23, 15, 52), groups[0].endMillis)
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

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()
    }
}
