package app.meeplebook.core.timer.service

import app.meeplebook.core.timer.model.ActivePlayTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PlayTimerNotificationFormatterTest {
    @Test
    fun `formatPlayTimerElapsed formats durations under one hour as mm ss`() {
        assertEquals("00:00", formatPlayTimerElapsed(Duration.ZERO))
        assertEquals("05:07", formatPlayTimerElapsed(Duration.ofSeconds(307)))
    }

    @Test
    fun `formatPlayTimerElapsed formats durations of one hour or more as h mm ss`() {
        assertEquals("1:02:00", formatPlayTimerElapsed(Duration.ofMinutes(62)))
        assertEquals("3:00:00", formatPlayTimerElapsed(Duration.ofHours(3)))
    }

    @Test
    fun `formatPlayTimerElapsed clamps negative durations to zero`() {
        assertEquals("00:00", formatPlayTimerElapsed(Duration.ofSeconds(-30)))
    }

    @Test
    fun `buildPlayTimerNotificationTiming uses chronometer while running`() {
        val now = Instant.parse("2026-03-01T10:10:00Z")
        val timer = ActivePlayTimer(
            startedAt = Instant.parse("2026-03-01T10:00:00Z"),
            accumulated = Duration.ZERO,
            isRunning = true,
            hasStarted = true,
        )

        val timing = buildPlayTimerNotificationTiming(timer, now)

        assertTrue(timing.usesChronometer)
        assertNull(timing.contentText)
        assertEquals(Instant.parse("2026-03-01T10:00:00Z").toEpochMilli(), timing.whenMillis)
    }

    @Test
    fun `buildPlayTimerNotificationTiming shows static text while paused`() {
        val now = Instant.parse("2026-03-01T10:10:00Z")
        val timer = ActivePlayTimer(
            startedAt = null,
            accumulated = Duration.ofMinutes(12).plusSeconds(5),
            isRunning = false,
            hasStarted = true,
        )

        val timing = buildPlayTimerNotificationTiming(timer, now)

        assertFalse(timing.usesChronometer)
        assertEquals(now.toEpochMilli(), timing.whenMillis)
        assertEquals("12:05", timing.contentText)
    }
}
