package app.meeplebook.feature.addplay.ui

import app.meeplebook.feature.addplay.ui.dialogs.handleNumpadKey
import app.meeplebook.feature.addplay.ui.dialogs.toScoreInputString
import app.meeplebook.feature.addplay.ui.dialogs.toScoreOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreInputDialogTest {

    // ── toScoreInputString ────────────────────────────────────────────────────

    @Test
    fun `toScoreInputString null returns empty string`() {
        val result = null.toScoreInputString()
        assertEquals("", result)
    }

    @Test
    fun `toScoreInputString whole number strips dot zero`() {
        assertEquals("42", 42.0.toScoreInputString())
    }

    @Test
    fun `toScoreInputString zero returns zero string`() {
        assertEquals("0", 0.0.toScoreInputString())
    }

    @Test
    fun `toScoreInputString decimal preserved`() {
        assertEquals("42.5", 42.5.toScoreInputString())
    }

    @Test
    fun `toScoreInputString negative whole number strips dot zero`() {
        assertEquals("-10", (-10.0).toScoreInputString())
    }

    @Test
    fun `toScoreInputString negative decimal preserved`() {
        assertEquals("-10.5", (-10.5).toScoreInputString())
    }

    // ── toScoreOrNull ─────────────────────────────────────────────────────────

    @Test
    fun `toScoreOrNull empty string returns null`() {
        assertNull("".toScoreOrNull())
    }

    @Test
    fun `toScoreOrNull minus only returns null`() {
        assertNull("-".toScoreOrNull())
    }

    @Test
    fun `toScoreOrNull blank string returns null`() {
        assertNull("   ".toScoreOrNull())
    }

    @Test
    fun `toScoreOrNull valid integer string returns double`() {
        assertEquals(42.0, "42".toScoreOrNull())
    }

    @Test
    fun `toScoreOrNull valid decimal string returns double`() {
        assertEquals(42.5, "42.5".toScoreOrNull())
    }

    @Test
    fun `toScoreOrNull negative number returns correct double`() {
        assertEquals(-10.0, "-10".toScoreOrNull())
    }

    @Test
    fun `toScoreOrNull negative decimal returns correct double`() {
        assertEquals(-10.5, "-10.5".toScoreOrNull())
    }

    // ── handleNumpadKey ───────────────────────────────────────────────────────

    @Test
    fun `digit appended to empty input`() {
        assertEquals("5", handleNumpadKey("5", ""))
    }

    @Test
    fun `digits build a multi-digit number`() {
        var input = ""
        listOf("1", "2", "3").forEach { input = handleNumpadKey(it, input) }
        assertEquals("123", input)
    }

    @Test
    fun `leading zero not duplicated when adding digit`() {
        assertEquals("5", handleNumpadKey("5", "0"))
    }

    @Test
    fun `leading zero preserved when adding decimal`() {
        assertEquals("0.", handleNumpadKey(".", "0"))
    }

    @Test
    fun `decimal added to integer input`() {
        assertEquals("42.", handleNumpadKey(".", "42"))
    }

    @Test
    fun `second decimal ignored`() {
        assertEquals("42.5", handleNumpadKey(".", "42.5"))
    }

    @Test
    fun `plus-minus toggles positive to negative`() {
        assertEquals("-42", handleNumpadKey("+/-", "42"))
    }

    @Test
    fun `plus-minus toggles negative to positive`() {
        assertEquals("42", handleNumpadKey("+/-", "-42"))
    }

    @Test
    fun `plus-minus on empty input does nothing`() {
        assertEquals("", handleNumpadKey("+/-", ""))
    }

    @Test
    fun `plus-minus on negative zero produces correct result`() {
        assertEquals("-5", handleNumpadKey("5", "-0"))
    }

    @Test
    fun `digit after negative zero replaces zero`() {
        assertEquals("-7", handleNumpadKey("7", "-0"))
    }
}
