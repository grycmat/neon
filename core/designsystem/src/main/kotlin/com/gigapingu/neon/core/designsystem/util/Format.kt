package com.gigapingu.neon.core.designsystem.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val shortDate = DateTimeFormatter.ofPattern("MMM d")
private val fullDate = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")

/** "4m", "2h", "3d", else a short date — matches the design's timestamps. */
fun relativeTime(time: Instant?, now: Instant = Instant.now()): String {
    if (time == null) return ""
    val seconds = (now.epochSecond - time.epochSecond).coerceAtLeast(0)
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3_600 -> "${seconds / 60}m"
        seconds < 86_400 -> "${seconds / 3_600}h"
        seconds < 7 * 86_400 -> "${seconds / 86_400}d"
        else -> shortDate.format(time.atZone(ZoneId.systemDefault()))
    }
}

fun fullTime(time: Instant?): String =
    time?.let { fullDate.format(it.atZone(ZoneId.systemDefault())) } ?: ""

/** 1234 → "1.2K" */
fun compactCount(n: Int): String = when {
    n < 1_000 -> "$n"
    n < 1_000_000 -> trimmed(n / 1_000.0) + "K"
    else -> trimmed(n / 1_000_000.0) + "M"
}

private fun trimmed(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}" else "$rounded"
}

/** Time remaining on a poll: "22h left" / "Closed". */
fun pollTimeLeft(expiresAt: Instant?, expired: Boolean, now: Instant = Instant.now()): String {
    if (expired || expiresAt == null) return "Closed"
    val seconds = expiresAt.epochSecond - now.epochSecond
    if (seconds <= 0) return "Closed"
    return when {
        seconds >= 86_400 -> "${seconds / 86_400}d left"
        seconds >= 3_600 -> "${seconds / 3_600}h left"
        else -> "${seconds / 60}m left"
    }
}
