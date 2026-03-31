package app.meeplebook.feature.addplay.ui

import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.ui.dialogs.remainingColors
import app.meeplebook.feature.addplay.ui.dialogs.sortedHistoryColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPickerDialogTest {

    // ── sortedHistoryColors ───────────────────────────────────────────────────

    @Test
    fun `sortedHistoryColors returns empty list when history is empty`() {
        val result = sortedHistoryColors(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `sortedHistoryColors returns colors sorted by enum ordinal`() {
        val unsorted = listOf(PlayerColor.PINK, PlayerColor.RED, PlayerColor.BLUE)
        val result = sortedHistoryColors(unsorted)
        assertEquals(listOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.PINK), result)
    }

    @Test
    fun `sortedHistoryColors preserves single-item list`() {
        val result = sortedHistoryColors(listOf(PlayerColor.GREEN))
        assertEquals(listOf(PlayerColor.GREEN), result)
    }

    @Test
    fun `sortedHistoryColors stable when already sorted`() {
        val sorted = listOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN)
        assertEquals(sorted, sortedHistoryColors(sorted))
    }

    // ── remainingColors ────────────────────────────────────────────────────────

    @Test
    fun `remainingColors with empty history returns all colors sorted by ordinal`() {
        val result = remainingColors(emptyList())
        assertEquals(PlayerColor.entries.sortedBy { it.ordinal }, result)
    }

    @Test
    fun `remainingColors excludes colors present in history`() {
        val history = listOf(PlayerColor.RED, PlayerColor.BLUE)
        val result = remainingColors(history)
        assertFalse(result.contains(PlayerColor.RED))
        assertFalse(result.contains(PlayerColor.BLUE))
    }

    @Test
    fun `remainingColors includes all colors not in history`() {
        val history = listOf(PlayerColor.RED, PlayerColor.BLUE)
        val result = remainingColors(history)
        val expected = PlayerColor.entries
            .filter { it != PlayerColor.RED && it != PlayerColor.BLUE }
            .sortedBy { it.ordinal }
        assertEquals(expected, result)
    }

    @Test
    fun `remainingColors returns empty when all colors are in history`() {
        val result = remainingColors(PlayerColor.entries.toList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `remainingColors is sorted by enum ordinal`() {
        val history = listOf(PlayerColor.TEAL, PlayerColor.PINK)
        val result = remainingColors(history)
        // Check each consecutive pair is in ordinal order.
        result.zipWithNext { a, b -> assertTrue(a.ordinal < b.ordinal) }
    }

    // ── history + remaining together ──────────────────────────────────────────

    @Test
    fun `history and remaining together cover all colors exactly once`() {
        val history = listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
        val sorted = sortedHistoryColors(history)
        val remaining = remainingColors(history)
        val combined = sorted + remaining
        assertEquals(PlayerColor.entries.size, combined.size)
        assertEquals(PlayerColor.entries.toSet(), combined.toSet())
    }

    @Test
    fun `history and remaining have no overlap`() {
        val history = listOf(PlayerColor.BLUE, PlayerColor.PURPLE)
        val sorted = sortedHistoryColors(history)
        val remaining = remainingColors(history)
        val overlap = sorted.intersect(remaining.toSet())
        assertTrue(overlap.isEmpty())
    }
}
