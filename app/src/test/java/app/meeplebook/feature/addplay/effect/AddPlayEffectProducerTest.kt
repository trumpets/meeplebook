package app.meeplebook.feature.addplay.effect

import app.meeplebook.R
import app.meeplebook.core.ui.UiText
import app.meeplebook.feature.addplay.AddPlayEffect
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeState
import app.meeplebook.feature.addplay.AddPlayUiEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddPlayEffectProducerTest {

    private val producer = AddPlayEffectProducer()

    // ── LocationChanged ──────────────────────────────────────────────────────

    @Test
    fun `LocationChanged emits LoadPlayerSuggestions with new location`() {
        val state = makeState(locationValue = "Home")
        val event = AddPlayEvent.MetadataEvent.LocationChanged("Game Cafe")

        val result = producer.produce(newState = state, event = event)

        assertEquals(1, result.effects.size)
        val effect = result.effects.first() as AddPlayEffect.LoadPlayerSuggestions
        assertEquals("Game Cafe", effect.location)
        assertEquals(state.gameId, effect.gameId)
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `LocationChanged with empty location emits LoadPlayerSuggestions with empty location`() {
        val state = makeState(locationValue = "Home")
        val event = AddPlayEvent.MetadataEvent.LocationChanged("")

        val result = producer.produce(newState = state, event = event)

        val effect = result.effects.first() as AddPlayEffect.LoadPlayerSuggestions
        assertEquals("", effect.location)
    }

    // ── SaveClicked ──────────────────────────────────────────────────────────

    @Test
    fun `SaveClicked with canSave true emits SavePlay domain effect`() {
        val state = makeState(gameName = "Wingspan").copy(canSave = true)
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        assertEquals(1, result.effects.size)
        assertTrue(result.effects.first() is AddPlayEffect.SavePlay)
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `SaveClicked SavePlay effect contains play with correct gameId and gameName`() {
        val state = makeState(gameId = 99L, gameName = "Terraforming Mars").copy(canSave = true)
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        val effect = result.effects.first() as AddPlayEffect.SavePlay
        assertEquals(99L, effect.play.gameId)
        assertEquals("Terraforming Mars", effect.play.gameName)
    }

    @Test
    fun `SaveClicked with canSave false emits ShowError ui effect`() {
        val state = makeState()
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertEquals(1, result.uiEffects.size)
        val uiEffect = result.uiEffects.first() as AddPlayUiEffect.ShowError
        assertEquals(UiText.Res(R.string.add_play_cant_save), uiEffect.message)
    }

    @Test
    fun `SaveClicked with null gameId emits ShowError ui effect`() {
        val state = makeState(gameId = null)
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertEquals(1, result.uiEffects.size)
        assertTrue(result.uiEffects.first() is AddPlayUiEffect.ShowError)
    }

    // ── CancelClicked ────────────────────────────────────────────────────────

    @Test
    fun `CancelClicked emits NavigateBack ui effect`() {
        val state = makeState()
        val event = AddPlayEvent.ActionEvent.CancelClicked

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertEquals(1, result.uiEffects.size)
        assertEquals(AddPlayUiEffect.NavigateBack, result.uiEffects.first())
    }

    // ── Unhandled events ─────────────────────────────────────────────────────

    @Test
    fun `DateChanged produces no effects`() {
        val state = makeState()
        val event = AddPlayEvent.MetadataEvent.DateChanged(state.date)

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `AddNewPlayer produces no effects`() {
        val state = makeState()
        val event = AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1)

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `LocationSuggestionSelected produces no effects`() {
        val state = makeState()
        val event = AddPlayEvent.MetadataEvent.LocationSuggestionSelected("Game Cafe")

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertTrue(result.uiEffects.isEmpty())
    }
}
