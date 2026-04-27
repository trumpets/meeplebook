package app.meeplebook.core.timer.domain

import app.meeplebook.core.timer.model.ActivePlayTimer
import app.meeplebook.core.timer.model.computeElapsed
import java.time.Duration
import java.time.Instant

/**
 * Pure state transitions for the global play timer.
 */
object PlayTimerStateMachine {

    fun start(
        playId: Long?,
        now: Instant,
    ): ActivePlayTimer {
        return ActivePlayTimer(
            playId = playId,
            startedAt = now,
            accumulated = Duration.ZERO,
            isRunning = true,
            hasStarted = true,
        )
    }

    fun pause(
        timer: ActivePlayTimer,
        now: Instant,
    ): ActivePlayTimer {
        if (!timer.hasStarted || !timer.isRunning || timer.startedAt == null) {
            return timer
        }

        return timer.copy(
            startedAt = null,
            accumulated = computeElapsed(timer, now),
            isRunning = false,
        )
    }

    fun resume(
        timer: ActivePlayTimer,
        now: Instant,
    ): ActivePlayTimer {
        if (!timer.hasStarted || timer.isRunning) {
            return timer
        }

        return timer.copy(
            startedAt = now,
            isRunning = true,
        )
    }

    fun reset(
        timer: ActivePlayTimer,
        now: Instant,
    ): ActivePlayTimer {
        if (!timer.hasStarted) {
            return timer
        }

        return timer.copy(
            startedAt = now,
            accumulated = Duration.ZERO,
            isRunning = true,
        )
    }
}
