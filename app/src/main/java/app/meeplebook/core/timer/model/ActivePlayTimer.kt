package app.meeplebook.core.timer.model

import java.time.Duration
import java.time.Instant

/**
 * Persisted state for the single global play timer.
 *
 * [hasStarted] distinguishes the initial "never started" state from a paused timer that happens to
 * have zero accumulated duration.
 */
data class ActivePlayTimer(
    val playId: Long? = null,
    val startedAt: Instant? = null,
    val accumulated: Duration = Duration.ZERO,
    val isRunning: Boolean = false,
    val hasStarted: Boolean = false,
)

/**
 * Derives the current elapsed duration from persisted timer state and a wall-clock instant.
 */
fun computeElapsed(
    timer: ActivePlayTimer,
    now: Instant,
): Duration {
    val runningSegment =
        if (timer.isRunning && timer.startedAt != null) {
            Duration.between(timer.startedAt, now).coerceAtLeast(Duration.ZERO)
        } else {
            Duration.ZERO
        }

    return (timer.accumulated + runningSegment).coerceAtLeast(Duration.ZERO)
}
