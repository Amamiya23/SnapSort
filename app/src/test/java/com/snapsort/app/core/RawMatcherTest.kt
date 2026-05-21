package com.snapsort.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawMatcherTest {
    @Test
    fun jpgDetectionIsCaseInsensitive() {
        assertTrue(isJpgFile("IMG_0001.JPG"))
        assertTrue(isJpgFile("IMG_0002.jpeg"))
        assertFalse(isJpgFile("IMG_0003.CR3"))
    }

    @Test
    fun matchesRawBySameBaseNameIgnoringCase() {
        val files = listOf(
            file("content://folder/IMG_0001.JPG", "IMG_0001.JPG"),
            file("content://folder/img_0001.CR3", "img_0001.CR3")
        )

        val matches = matchRawFiles(files)

        assertEquals("img_0001.CR3", matches.getValue("content://folder/IMG_0001.JPG").fileName)
    }

    @Test
    fun doesNotMatchFuzzySuffixes() {
        val files = listOf(
            file("content://folder/IMG_0001.JPG", "IMG_0001.JPG"),
            file("content://folder/IMG_0001_EDIT.CR3", "IMG_0001_EDIT.CR3")
        )

        val matches = matchRawFiles(files)

        assertFalse(matches.containsKey("content://folder/IMG_0001.JPG"))
    }

    @Test
    fun supportedRawDetectionIsCaseInsensitive() {
        assertTrue(isSupportedRawFile("IMG_0001.NEF"))
        assertTrue(isSupportedRawFile("IMG_0002.dng"))
        assertFalse(isSupportedRawFile("IMG_0003.tif"))
    }

    private fun file(uri: String, name: String): ScannedFile {
        return ScannedFile(uri = uri, fileName = name, modifiedAtMillis = 0L)
    }
}
