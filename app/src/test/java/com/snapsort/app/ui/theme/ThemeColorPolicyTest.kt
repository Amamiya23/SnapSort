package com.snapsort.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorPolicyTest {
    @Test
    fun defaultThemeDoesNotUseDynamicColor() {
        assertFalse(SnapSortDefaultDynamicColor)
    }

    @Test
    fun dynamicColorRequiresOptInAndSupportedSdk() {
        assertFalse(shouldUseDynamicColor(dynamicColor = false, sdkInt = 34))
        assertFalse(shouldUseDynamicColor(dynamicColor = true, sdkInt = 30))
        assertTrue(shouldUseDynamicColor(dynamicColor = true, sdkInt = 31))
    }
}
