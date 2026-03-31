package app.meeplebook.feature.addplay.effect

import app.meeplebook.R
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.core.ui.UiText
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSearchState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSelectedState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AddPlayEffectProducerTest {

    private val producer = AddPlayEffectProducer()

    // ── LocationChanged ──────────────────────────────────────────────────────

    @Test
    fun `LocationChanged emits LoadPlayerSuggestions with new location`() {
        val state = makeGameSelectedState(locationValue = "Home")
        val event = AddPlayEvent.MetadataEvent.LocationChanged("Game Cafe")

        val result = producer.produce(newState = state, event = event)

        assertEquals(1, result.effects.size)
        val effect = result.effects.first() as AddPlayEffect.LoadPlayerSuggestions
        assertEquals("Game Cafe", effect.location)
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `LocationChanged with GameSearch state produces no effects`() {
        val state = makeGameSearchState(gameId = null)
        val event = AddPlayEvent.MetadataEvent.LocationChanged("Game Cafe")

        val result = producer.produce(newState = state, event = event)

        assertNoEffects(result)
    }

    @Test
    fun `LocationChanged with empty location emits LoadPlayerSuggestions with empty location`() {
        val state = makeGameSelectedState(locationValue = "Home")
        val event = AddPlayEvent.MetadataEvent.LocationChanged("")

        val result = producer.produce(newState = state, event = event)

        val effect = result.effects.first() as AddPlayEffect.LoadPlayerSuggestions
        assertEquals("", effect.location)
    }

    @Test
    fun `LocationChanged forwards location to LoadPlayerSuggestions`() {
        val state = makeGameSelectedState(gameId = 77L)
        val event = AddPlayEvent.MetadataEvent.LocationChanged("Home")

        val result = producer.produce(newState = state, event = event)

        val effect = result.effects.first() as AddPlayEffect.LoadPlayerSuggestions
        assertEquals("Home", effect.location)
    }

    // ── SaveClicked — routing ────────────────────────────────────────────────

    @Test
    fun `SaveClicked with canSave true emits SavePlay domain effect`() {
        val state = makeGameSelectedState()
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        assertEquals(1, result.effects.size)
        assertTrue(result.effects.first() is AddPlayEffect.SavePlay)
        assertTrue(result.uiEffects.isEmpty())
    }

    @Test
    fun `SaveClicked with canSave false emits ShowError ui effect`() {
        val state = makeGameSelectedState(isSaving = true)
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertEquals(1, result.uiEffects.size)
        val uiEffect = result.uiEffects.first() as AddPlayUiEffect.ShowError
        assertEquals(UiText.Res(R.string.add_play_cant_save), uiEffect.message)
    }

    @Test
    fun `SaveClicked with GameSearch state emits no effects`() {
        val state = makeGameSearchState(gameId = null)
        val event = AddPlayEvent.ActionEvent.SaveClicked

        val result = producer.produce(newState = state, event = event)

        assertNoEffects(result)
    }

    // ── SaveClicked — SavePlay field mapping ─────────────────────────────────

    @Test
    fun `SaveClicked SavePlay maps gameId and gameName`() {
        val state = makeGameSelectedState(gameId = 99L, gameName = "Terraforming Mars")

        val play = savePlay(state)

        assertEquals(99L, play.gameId)
        assertEquals("Terraforming Mars", play.gameName)
    }

    @Test
    fun `SaveClicked SavePlay maps date`() {
        val date = Instant.parse("2025-07-15T14:30:00Z")
        val state = makeGameSelectedState(date = date)

        val play = savePlay(state)

        assertEquals(date, play.date)
    }

    @Test
    fun `SaveClicked SavePlay maps durationMinutes as length`() {
        val state = makeGameSelectedState(durationMinutes = 90)

        val play = savePlay(state)

        assertEquals(90, play.length)
    }

    @Test
    fun `SaveClicked SavePlay maps null durationMinutes as null length`() {
        val state = makeGameSelectedState(durationMinutes = null)

        val play = savePlay(state)

        assertNull(play.length)
    }

    @Test
    fun `SaveClicked SavePlay maps location value`() {
        val state = makeGameSelectedState(locationValue = "Game Cafe")

        val play = savePlay(state)

        assertEquals("Game Cafe", play.location)
    }

    @Test
    fun `SaveClicked SavePlay maps null location as null`() {
        val state = makeGameSelectedState(locationValue = null)

        val play = savePlay(state)

        assertNull(play.location)
    }

    @Test
    fun `SaveClicked SavePlay maps non-blank comments`() {
        val state = makeGameSelectedState().copy(comments = "Great game!")

        val play = savePlay(state)

        assertEquals("Great game!", play.comments)
    }

    @Test
    fun `SaveClicked SavePlay maps blank comments as null`() {
        val state = makeGameSelectedState().copy(comments = "")

        val play = savePlay(state)

        assertNull(play.comments)
    }

    @Test
    fun `SaveClicked SavePlay maps quantity`() {
        val state = makeGameSelectedState().copy(quantity = 3)

        val play = savePlay(state)

        assertEquals(3, play.quantity)
    }

    @Test
    fun `SaveClicked SavePlay maps incomplete flag`() {
        val state = makeGameSelectedState().copy(incomplete = true)

        val play = savePlay(state)

        assertEquals(true, play.incomplete)
    }

    @Test
    fun `SaveClicked SavePlay maps players with all fields`() {
        val identity = makeIdentity(name = "Alice", username = "alice99", userId = 42L)
        val player = makePlayer(identity = identity, startPosition = 2, score = 150.0, isWinner = true, color = "Red")
        val state = makeGameSelectedState(players = listOf(player))

        val play = savePlay(state)

        assertEquals(1, play.players.size)
        val cmd = play.players.first()
        assertEquals("Alice", cmd.name)
        assertEquals("alice99", cmd.username)
        assertEquals(42L, cmd.userId)
        assertEquals(2, cmd.startPosition)
        assertEquals(150, cmd.score)
        assertEquals("Red", cmd.color)
        assertEquals(true, cmd.win)
    }

    @Test
    fun `SaveClicked SavePlay filters out players with blank names`() {
        val namedPlayer = makePlayer(makeIdentity(name = "Alice"), startPosition = 1)
        val blankPlayer = makePlayer(makeIdentity(name = ""), startPosition = 2)
        val state = makeGameSelectedState(players = listOf(namedPlayer, blankPlayer))

        val play = savePlay(state)

        assertEquals(1, play.players.size)
        assertEquals("Alice", play.players.first().name)
    }

    @Test
    fun `SaveClicked SavePlay maps empty player list`() {
        val state = makeGameSelectedState(players = emptyList())

        val play = savePlay(state)

        assertTrue(play.players.isEmpty())
    }

    // ── CancelClicked ────────────────────────────────────────────────────────

    @Test
    fun `CancelClicked emits NavigateBack ui effect`() {
        val state = makeGameSelectedState()
        val event = AddPlayEvent.ActionEvent.CancelClicked

        val result = producer.produce(newState = state, event = event)

        assertTrue(result.effects.isEmpty())
        assertEquals(1, result.uiEffects.size)
        assertEquals(AddPlayUiEffect.NavigateBack, result.uiEffects.first())
    }

    // ── Unhandled events — no effects ────────────────────────────────────────

    @Test
    fun `DateChanged produces no effects`() {
        val state = makeGameSelectedState()
        val result = producer.produce(newState = state, event = AddPlayEvent.MetadataEvent.DateChanged(state.date))
        assertNoEffects(result)
    }

    @Test
    fun `DurationChanged produces no effects`() {
        val state = makeGameSelectedState()
        val result = producer.produce(newState = state, event = AddPlayEvent.MetadataEvent.DurationChanged(60))
        assertNoEffects(result)
    }

    @Test
    fun `AddNewPlayer produces no effects`() {
        val state = makeGameSelectedState()
        val result = producer.produce(newState = state, event = AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        assertNoEffects(result)
    }

    @Test
    fun `NameChanged produces no effects`() {
        val identity = makeIdentity("Alice")
        val state = makeGameSelectedState()
        val result = producer.produce(newState = state, event = AddPlayEvent.PlayerEditEvent.NameChanged(identity, "Alicia"))
        assertNoEffects(result)
    }

    @Test
    fun `ScoreChanged produces no effects`() {
        val identity = makeIdentity("Alice")
        val state = makeGameSelectedState()
        val result = producer.produce(newState = state, event = AddPlayEvent.PlayerScoreEvent.ScoreChanged(identity, 42))
        assertNoEffects(result)
    }

    @Test
    fun `ColorSelected produces no effects`() {
        val identity = makeIdentity("Alice")
        val state = makeGameSelectedState()
        val result = producer.produce(newState = state, event = AddPlayEvent.PlayerColorEvent.ColorSelected(identity, PlayerColor.RED))
        assertNoEffects(result)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun savePlay(state: app.meeplebook.feature.addplay.AddPlayUiState) =
        (producer.produce(newState = state, event = AddPlayEvent.ActionEvent.SaveClicked)
            .effects.first() as AddPlayEffect.SavePlay).play

    private fun assertNoEffects(result: AddPlayEffects) {
        assertTrue(result.effects.isEmpty())
        assertTrue(result.uiEffects.isEmpty())
    }
}
