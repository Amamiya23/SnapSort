package com.snapsort.app.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionComparatorTest {
    @Test
    fun sameTagIsUpToDate() {
        assertFalse(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2026.06.16-1200",
                currentVersionCode = 1_800_000_000,
                latestTagName = "v2026.06.16-1200"
            )
        )
    }

    @Test
    fun newerTimestampTagIsAvailable() {
        assertTrue(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2026.06.16-1100",
                currentVersionCode = 1_800_000_000,
                latestTagName = "v2026.06.16-1200"
            )
        )
    }

    @Test
    fun olderTimestampTagIsUpToDate() {
        assertFalse(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2026.06.16-1300",
                currentVersionCode = 1_800_000_000,
                latestTagName = "v2026.06.16-1200"
            )
        )
    }

    @Test
    fun newerSemanticTagIsAvailable() {
        assertTrue(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2.0",
                currentVersionCode = 3,
                latestTagName = "v2.1"
            )
        )
    }

    @Test
    fun olderSemanticTagIsUpToDate() {
        assertFalse(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2.2",
                currentVersionCode = 4,
                latestTagName = "v2.1"
            )
        )
    }

    @Test
    fun numericTagComparesAgainstVersionCode() {
        assertTrue(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2.0",
                currentVersionCode = 10,
                latestTagName = "11"
            )
        )
    }

    @Test
    fun blankLatestTagIsUpToDate() {
        assertFalse(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2.0",
                currentVersionCode = 3,
                latestTagName = " "
            )
        )
    }

    @Test
    fun unrecognizedDifferentLatestTagIsAvailable() {
        assertTrue(
            UpdateVersionComparator.isUpdateAvailable(
                currentVersionName = "v2.0",
                currentVersionCode = 3,
                latestTagName = "release-candidate"
            )
        )
    }
}
