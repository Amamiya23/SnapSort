package com.snapsort.app.ui.copy

fun burstThresholdLabel(thresholdMillis: Long): String = when (thresholdMillis) {
    500L -> "0.5 秒"
    1_000L -> "1 秒"
    2_000L -> "2 秒"
    3_000L -> "3 秒"
    5_000L -> "5 秒"
    else -> "${thresholdMillis / 1_000.0} 秒"
}

fun gestureShortcutDescription(): String =
    "下滑超过提示阈值才会标记删除，上滑取消标记。按钮操作始终保留，最终删除前仍会确认。"
