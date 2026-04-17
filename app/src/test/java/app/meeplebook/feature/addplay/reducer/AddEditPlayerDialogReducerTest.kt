package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.feature.addplay.AddEditPlayerDialogState
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSearchState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSelectedState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.requireGameSearch
import app.meeplebook.feature.addplay.requireGameSelected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEditPlayerDialogReducerTest {

    private val reducer = AddEditPlayerDialogReducer()

    // region ShowAddPlayerDialog

    @Test
    fun `ShowAddPlayerDialog opens dialog with empty state`() {
        val state = makeGameSelectedState()
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ShowAddPlayerDialog)
            .requireGameSelected()
        assertNotNull(result.addEditPlayerDialog)
        val dialog = result.addEditPlayerDialog!!
        assertNull(dialog.editingIdentity)
        assertEquals("", dialog.name)
        assertEquals("", dialog.username)
        assertEquals("", dialog.color)
    }

    @Test
    fun `ShowAddPlayerDialog replaces any existing dialog state`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Old Name")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ShowAddPlayerDialog)
            .requireGameSelected()
        assertEquals("", result.addEditPlayerDialog!!.name)
    }

    // endregion

    // region ShowEditPlayerDialog

    @Test
    fun `ShowEditPlayerDialog opens dialog pre-filled with player identity`() {
        val alice = makeIdentity("Alice", username = "alice_bgg")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice, color = "Blue")))

        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ShowEditPlayerDialog(alice))
            .requireGameSelected()

        val dialog = result.addEditPlayerDialog!!
        assertEquals(alice, dialog.editingIdentity)
        assertEquals("Alice", dialog.name)
        assertEquals("alice_bgg", dialog.username)
        assertEquals("Blue", dialog.color)
    }

    @Test
    fun `ShowEditPlayerDialog with player without username leaves username blank`() {
        val bob = makeIdentity("Bob", username = null)
        val state = makeGameSelectedState(players = listOf(makePlayer(bob)))

        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ShowEditPlayerDialog(bob))
            .requireGameSelected()

        assertEquals("", result.addEditPlayerDialog!!.username)
    }

    @Test
    fun `ShowEditPlayerDialog with player without color leaves color blank`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice, color = null)))

        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ShowEditPlayerDialog(alice))
            .requireGameSelected()

        assertEquals("", result.addEditPlayerDialog!!.color)
    }

    // endregion

    // region DismissAddEditPlayerDialog

    @Test
    fun `DismissAddEditPlayerDialog closes the dialog`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.DismissAddEditPlayerDialog)
            .requireGameSelected()
        assertNull(result.addEditPlayerDialog)
    }

    @Test
    fun `DismissAddEditPlayerDialog when dialog already null is a noop`() {
        val state = makeGameSelectedState()
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.DismissAddEditPlayerDialog)
            .requireGameSelected()
        assertNull(result.addEditPlayerDialog)
    }

    // endregion

    // region AddEditNameChanged

    @Test
    fun `AddEditNameChanged updates name and clears name suggestions`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(
                name = "Al",
                nameSuggestions = listOf(makeIdentity("Alice"), makeIdentity("Alex"))
            )
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditNameChanged("Alice"))
            .requireGameSelected()
        assertEquals("Alice", result.addEditPlayerDialog!!.name)
        assertEquals(emptyList<PlayerIdentity>(), result.addEditPlayerDialog!!.nameSuggestions)
    }

    @Test
    fun `AddEditNameChanged does not affect username`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(username = "alice_bgg")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditNameChanged("Alice"))
            .requireGameSelected()
        assertEquals("alice_bgg", result.addEditPlayerDialog!!.username)
    }

    @Test
    fun `AddEditNameChanged when dialog is null is a noop`() {
        val state = makeGameSelectedState()
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditNameChanged("Alice"))
            .requireGameSelected()
        assertNull(result.addEditPlayerDialog)
    }

    // endregion

    // region AddEditUsernameChanged

    @Test
    fun `AddEditUsernameChanged updates username and clears username suggestions`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(
                username = "al",
                usernameSuggestions = listOf(makeIdentity("Alice", username = "alice_bgg"))
            )
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditUsernameChanged("alice_bgg"))
            .requireGameSelected()
        assertEquals("alice_bgg", result.addEditPlayerDialog!!.username)
        assertEquals(emptyList<PlayerIdentity>(), result.addEditPlayerDialog!!.usernameSuggestions)
    }

    @Test
    fun `AddEditUsernameChanged does not affect name`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditUsernameChanged("alice_bgg"))
            .requireGameSelected()
        assertEquals("Alice", result.addEditPlayerDialog!!.name)
    }

    // endregion

    // region AddEditColorChanged

    @Test
    fun `AddEditColorChanged updates color in dialog`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(color = "Red")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditColorChanged("Blue"))
            .requireGameSelected()
        assertEquals("Blue", result.addEditPlayerDialog!!.color)
    }

    @Test
    fun `AddEditColorChanged when dialog is null is a noop`() {
        val state = makeGameSelectedState()
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.AddEditColorChanged("Blue"))
            .requireGameSelected()
        assertNull(result.addEditPlayerDialog)
    }

    // endregion

    // region ConfirmAddEditPlayer — add mode

    @Test
    fun `ConfirmAddEditPlayer in add mode appends new player to list`() {
        val state = makeGameSelectedState(players = emptyList()).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice", username = "alice_bgg")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals(1, result.players.players.size)
        assertEquals("Alice", result.players.players.first().playerIdentity.name)
        assertEquals("alice_bgg", result.players.players.first().playerIdentity.username)
    }

    @Test
    fun `ConfirmAddEditPlayer in add mode closes dialog`() {
        val state = makeGameSelectedState(players = emptyList()).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertNull(result.addEditPlayerDialog)
    }

    @Test
    fun `ConfirmAddEditPlayer in add mode capitalizes each word of the name`() {
        val state = makeGameSelectedState(players = emptyList()).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "john paul")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals("John Paul", result.players.players.first().playerIdentity.name)
    }

    @Test
    fun `ConfirmAddEditPlayer in add mode normalizes blank username to null`() {
        val state = makeGameSelectedState(players = emptyList()).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice", username = "   ")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertNull(result.players.players.first().playerIdentity.username)
    }

    @Test
    fun `ConfirmAddEditPlayer in add mode assigns valid enum color as colorString`() {
        val state = makeGameSelectedState(players = emptyList()).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice", color = "blue")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals("Blue", result.players.players.first().color)
    }

    @Test
    fun `ConfirmAddEditPlayer in add mode with blank name closes dialog without adding player`() {
        val state = makeGameSelectedState(players = emptyList()).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "   ")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals(0, result.players.players.size)
        assertNull(result.addEditPlayerDialog)
    }

    @Test
    fun `ConfirmAddEditPlayer sets startPosition to players size + 1`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice))).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Bob")
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals(2, result.players.players.last().startPosition)
    }

    // endregion

    // region ConfirmAddEditPlayer — edit mode

    @Test
    fun `ConfirmAddEditPlayer in edit mode updates matching player identity`() {
        val alice = makeIdentity("Alice", username = "alice_bgg")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice))).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(
                editingIdentity = alice,
                name = "Alicia",
                username = "alicia_bgg"
            )
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals(1, result.players.players.size)
        assertEquals("Alicia", result.players.players.first().playerIdentity.name)
        assertEquals("alicia_bgg", result.players.players.first().playerIdentity.username)
    }

    @Test
    fun `ConfirmAddEditPlayer in edit mode does not change other players`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice), makePlayer(bob))).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(
                editingIdentity = alice,
                name = "Alicia"
            )
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals("Bob", result.players.players.last().playerIdentity.name)
    }

    @Test
    fun `ConfirmAddEditPlayer in edit mode closes dialog`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice))).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(
                editingIdentity = alice,
                name = "Alicia"
            )
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertNull(result.addEditPlayerDialog)
    }

    @Test
    fun `ConfirmAddEditPlayer in edit mode preserves userId from editing identity`() {
        val alice = makeIdentity("Alice", userId = 42L)
        val state = makeGameSelectedState(players = listOf(makePlayer(alice))).copy(
            addEditPlayerDialog = AddEditPlayerDialogState(
                editingIdentity = alice,
                name = "Alicia"
            )
        )
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals(42L, result.players.players.first().playerIdentity.userId)
    }

    // endregion

    // region ConfirmAddEditPlayer — no dialog open

    @Test
    fun `ConfirmAddEditPlayer when dialog is null closes dialog without side effects`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice)))
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            .requireGameSelected()
        assertEquals(1, result.players.players.size)
        assertNull(result.addEditPlayerDialog)
    }

    // endregion

    // region Unhandled events

    @Test
    fun `non-AddEditPlayerDialogEvent events leave dialog state unchanged`() {
        val state = makeGameSelectedState().copy(
            addEditPlayerDialog = AddEditPlayerDialogState(name = "Alice")
        )
        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DurationChanged(60))
            .requireGameSelected()
        assertEquals("Alice", result.addEditPlayerDialog!!.name)
    }

    @Test
    fun `events on GameSearch state return state unchanged`() {
        val state = makeGameSearchState()
        val result = reducer.reduce(state, AddPlayEvent.AddEditPlayerDialogEvent.ShowAddPlayerDialog)
        assertEquals(state, result)
        assertTrue(result is AddPlayUiState.GameSearch)
    }

    // endregion
}
