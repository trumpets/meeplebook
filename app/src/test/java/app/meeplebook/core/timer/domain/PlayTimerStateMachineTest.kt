package app.meeplebook.core.timer.domain

import app.meeplebook.core.timer.model.ActivePlayTimer
import app.meeplebook.core.timer.model.computeElapsed
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayTimerStateMachineTest {

    private val startedAt = Instant.parse("2026-03-01T10:00:00Z")

    @Test
    fun `start resets elapsed state and marks timer running`() {
        val timer = PlayTimerStateMachine.start(playId = 42L, now = startedAt)

        assertEquals(42L, timer.playId)
        assertEquals(startedAt, timer.startedAt)
        assertEquals(Duration.ZERO, timer.accumulated)
        assertTrue(timer.isRunning)
        assertTrue(timer.hasStarted)
    }

    @Test
    fun `pause accumulates elapsed running segment`() {
        val timer = PlayTimerStateMachine.start(playId = 7L, now = startedAt)

        val paused = PlayTimerStateMachine.pause(timer, now = startedAt.plusSeconds(125))

        assertFalse(paused.isRunning)
        assertEquals(null, paused.startedAt)
        assertEquals(Duration.ofSeconds(125), paused.accumulated)
        assertTrue(paused.hasStarted)
    }

    @Test
    fun `resume keeps accumulated time and restarts from current instant`() {
        val paused = ActivePlayTimer(
            playId = 7L,
            startedAt = null,
            accumulated = Duration.ofMinutes(9),
            isRunning = false,
            hasStarted = true,
        )

        val resumed = PlayTimerStateMachine.resume(paused, now = startedAt)

        assertEquals(7L, resumed.playId)
        assertEquals(startedAt, resumed.startedAt)
        assertEquals(Duration.ofMinutes(9), resumed.accumulated)
        assertTrue(resumed.isRunning)
        assertTrue(resumed.hasStarted)
    }

    @Test
    fun `reset clears elapsed time and starts running immediately`() {
        val paused = ActivePlayTimer(
            playId = 9L,
            startedAt = null,
            accumulated = Duration.ofMinutes(12),
            isRunning = false,
            hasStarted = true,
        )

        val reset = PlayTimerStateMachine.reset(paused, now = startedAt)

        assertEquals(9L, reset.playId)
        assertEquals(startedAt, reset.startedAt)
        assertEquals(Duration.ZERO, reset.accumulated)
        assertTrue(reset.isRunning)
        assertTrue(reset.hasStarted)
    }

    @Test
    fun `pause before timer has started is a no-op`() {
        val initial = ActivePlayTimer()

        assertEquals(initial, PlayTimerStateMachine.pause(initial, now = startedAt))
    }

    @Test
    fun `reset before timer has started is a no-op`() {
        val initial = ActivePlayTimer()

        assertEquals(initial, PlayTimerStateMachine.reset(initial, now = startedAt))
    }

    @Test
    fun `computeElapsed adds running segment only while timer is running`() {
        val running = ActivePlayTimer(
            playId = null,
            startedAt = startedAt,
            accumulated = Duration.ofMinutes(3),
            isRunning = true,
            hasStarted = true,
        )
        val paused = running.copy(startedAt = null, isRunning = false)

        assertEquals(Duration.ofMinutes(5), computeElapsed(running, startedAt.plusSeconds(120)))
        assertEquals(Duration.ofMinutes(3), computeElapsed(paused, startedAt.plusSeconds(120)))
    }

    @Test
    fun `computeElapsed never returns a negative duration`() {
        val running = ActivePlayTimer(
            playId = null,
            startedAt = startedAt,
            accumulated = Duration.ZERO,
            isRunning = true,
            hasStarted = true,
        )

        assertEquals(Duration.ZERO, computeElapsed(running, startedAt.minusSeconds(30)))
    }
}
