package com.snapsort.app.ui.copy

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

class TimeCopyTest {
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
    fun formatLocalPhotoTimeUsesSystemTimeZoneAndFullDate() {
        val millis = LocalDateTime.of(2026, 5, 23, 16, 20)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()

        assertEquals("2026-05-23 16:20", formatLocalPhotoTime(millis))
    }

    @Test
    fun formatLocalTimeRangeShowsShortDateAndOmitsDuplicateEndTime() {
        val millis = LocalDateTime.of(2026, 5, 23, 16, 20)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()

        assertEquals("05-23 16:20", formatLocalTimeRange(millis, millis))
    }

    @Test
    fun formatLocalTimeRangeShowsOneDateForSameDayRange() {
        val start = LocalDateTime.of(2026, 5, 23, 15, 30)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()
        val end = LocalDateTime.of(2026, 5, 23, 15, 52)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()

        assertEquals("05-23 15:30-15:52", formatLocalTimeRange(start, end))
    }

    @Test
    fun formatLocalTimeRangeShowsBothDatesForCrossDayRange() {
        val start = LocalDateTime.of(2026, 5, 23, 23, 50)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()
        val end = LocalDateTime.of(2026, 5, 24, 0, 10)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()

        assertEquals("05-23 23:50-05-24 00:10", formatLocalTimeRange(start, end))
    }
}
