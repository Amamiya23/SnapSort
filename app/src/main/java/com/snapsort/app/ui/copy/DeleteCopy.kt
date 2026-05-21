package com.snapsort.app.ui.copy

fun deleteConfirmationBody(totalFiles: Int): String =
    "接下来会交给 Android 系统确认删除 $totalFiles 个文件。取消系统确认不会改变已标记照片。"

fun deleteResultSummary(successCount: Int, failureCount: Int): String =
    if (failureCount == 0) {
        "已删除 $successCount 个文件。已成功删除的照片会从当前任务中移除。"
    } else {
        "已删除 $successCount 个文件，$failureCount 个文件仍保留标记。你可以重试，或取消这些标记后继续筛选。"
    }
