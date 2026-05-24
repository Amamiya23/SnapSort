package com.snapsort.app.ui.copy

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatLocalPhotoTime(value: Long): String {
    return Instant.ofEpochMilli(value)
        .atZone(ZoneId.systemDefault())
        .format(dateTimeFormatter)
}

fun formatLocalTimeRange(startMillis: Long, endMillis: Long): String {
    val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault())
    val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault())
    val startText = "${start.format(dateFormatter)} ${start.format(timeFormatter)}"
    val endText = if (start.toLocalDate() == end.toLocalDate()) {
        end.format(timeFormatter)
    } else {
        "${end.format(dateFormatter)} ${end.format(timeFormatter)}"
    }
    return if (startText.endsWith(endText)) startText else "$startText-$endText"
}
