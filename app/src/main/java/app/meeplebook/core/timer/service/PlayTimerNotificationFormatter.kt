package app.meeplebook.core.timer.service

import app.meeplebook.core.timer.model.ActivePlayTimer
import app.meeplebook.core.timer.model.computeElapsed
import java.time.Duration
import java.time.Instant

/**
 * Pure helpers for representing timer state inside the persistent notification.
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

data class PlayTimerNotificationTiming(
    val usesChronometer: Boolean,
    val whenMillis: Long,
    val contentText: String?,
)

fun buildPlayTimerNotificationTiming(
    timer: ActivePlayTimer,
    now: Instant,
): PlayTimerNotificationTiming {
    val elapsed = computeElapsed(timer, now)

    return if (timer.isRunning) {
        PlayTimerNotificationTiming(
            usesChronometer = true,
            whenMillis = now.toEpochMilli() - elapsed.toMillis(),
            contentText = null,
        )
    } else {
        PlayTimerNotificationTiming(
            usesChronometer = false,
            whenMillis = now.toEpochMilli(),
            contentText = formatPlayTimerElapsed(elapsed),
        )
    }
}
