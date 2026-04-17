package app.meeplebook.core.ui.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProducedEffectsTest {

    @Test
    fun `none returns empty effects bundle`() {
        val effects = ProducedEffects.none<String, Int>()

        assertTrue(effects.effects.isEmpty())
        assertTrue(effects.uiEffects.isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `isEmpty is false when any effect list has values`() {
        val domainEffects = ProducedEffects<String, Int>(effects = listOf("refresh"))
        val uiEffects = ProducedEffects<String, Int>(uiEffects = listOf(1))

        assertFalse(domainEffects.isEmpty())
        assertFalse(uiEffects.isEmpty())
    }

    @Test
    fun `bundle preserves effect ordering`() {
        val effects = ProducedEffects(
            effects = listOf("first", "second"),
            uiEffects = listOf(10, 20)
        )

        assertEquals(listOf("first", "second"), effects.effects)
        assertEquals(listOf(10, 20), effects.uiEffects)
    }
}
