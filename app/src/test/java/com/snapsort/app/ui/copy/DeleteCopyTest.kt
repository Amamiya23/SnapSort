package com.snapsort.app.ui.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DeleteCopyTest {
    @Test
    fun confirmationBodyExplainsSystemConfirmationAndKeepsMarksOnCancel() {
        val copy = deleteConfirmationBody(totalFiles = 7)

        assertEquals("接下来会交给 Android 系统确认删除 7 个文件。取消系统确认不会改变已标记照片。", copy)
        assertFalse(copy.contains("彻底删除"))
    }

    @Test
    fun deleteResultSummaryExplainsFailedItemsRemainMarked() {
        val copy = deleteResultSummary(successCount = 3, failureCount = 2)

        assertEquals("已删除 3 个文件，2 个文件仍保留标记。你可以重试，或取消这些标记后继续筛选。", copy)
    }

    @Test
    fun deleteResultSuccessSummaryConfirmsReturnState() {
        val copy = deleteResultSummary(successCount = 5, failureCount = 0)

        assertEquals("已删除 5 个文件。已成功删除的照片会从当前任务中移除。", copy)
    }
}
