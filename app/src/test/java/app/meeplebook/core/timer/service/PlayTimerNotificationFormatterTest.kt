package app.meeplebook.core.timer.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

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
}
