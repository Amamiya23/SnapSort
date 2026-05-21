package com.snapsort.app.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCopyTest {
    @Test
    fun defaultBurstThresholdLabelMarksRecommendedOption() {
        assertEquals("1 秒", burstThresholdLabel(1_000L))
    }

    @Test
    fun nonDefaultBurstThresholdLabelKeepsPlainValue() {
        assertEquals("2 秒", burstThresholdLabel(2_000L))
    }

    @Test
    fun gestureShortcutDescriptionExplainsThresholdAndConfirmation() {
        assertEquals(
            "下滑超过提示阈值才会标记删除，上滑取消标记。按钮操作始终保留，最终删除前仍会确认。",
            gestureShortcutDescription()
        )
    }
}
