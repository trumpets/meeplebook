package app.meeplebook.feature.plays.effect

import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.feature.plays.PlaysBaseState
import app.meeplebook.feature.plays.PlaysEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaysEffectProducerTest {

    private val producer = PlaysEffectProducer()

    @Test
    fun `PlayClicked emits NavigateToPlay ui effect`() {
        val result = producer.produce(
            newState = PlaysBaseState(),
            event = PlaysEvent.ActionEvent.PlayClicked(PlayId.Local(7L))
        )

        assertTrue(result.effects.isEmpty())
        assertEquals(1, result.uiEffects.size)
        assertTrue(result.uiEffects.first() is PlaysUiEffect.NavigateToPlay)
    }

    @Test
    fun `Refresh emits Refresh domain effect`() {
        val result = producer.produce(
            newState = PlaysBaseState(),
            event = PlaysEvent.ActionEvent.Refresh
        )

        assertEquals(listOf(PlaysEffect.Refresh), result.effects)
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `SearchChanged emits no effects`() {
        val result = producer.produce(
            newState = PlaysBaseState(),
            event = PlaysEvent.SearchChanged("wingspan")
        )

        assertEquals(PlaysEffects.None, result)
    }

    @Test
    fun `LogPlayClicked emits no effects`() {
        val result = producer.produce(
            newState = PlaysBaseState(),
            event = PlaysEvent.ActionEvent.LogPlayClicked
        )

        assertEquals(PlaysEffects.None, result)
    }
}
