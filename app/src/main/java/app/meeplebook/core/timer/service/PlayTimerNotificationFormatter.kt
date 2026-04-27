package app.meeplebook.core.timer.service

import java.time.Duration

/**
 * Formats elapsed timer duration for the persistent notification.
 */
fun formatPlayTimerElapsed(duration: Duration): String {
    val safeDuration = duration.coerceAtLeast(Duration.ZERO)
    val totalSeconds = safeDuration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString(if (hours > 0) 8 else 5) {
        if (hours > 0) {
            append(hours)
            append(':')
        }
        append(minutes.toString().padStart(2, '0'))
        append(':')
        append(seconds.toString().padStart(2, '0'))
    }
}
