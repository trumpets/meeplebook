package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.AddEditPlayerDialogState
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.feature.addplay.updateGameSelected
import javax.inject.Inject

/**
 * Reduces [AddPlayEvent.AddEditPlayerDialogEvent] events.
 *
 * Manages the lifecycle of the Add/Edit Player dialog:
 * - Opening in add-new mode or pre-filled edit mode.
 * - Updating individual fields (name, username, color) as the user types.
 * - Confirming: adds a new [PlayerEntryUi] or updates an existing one, then closes the dialog.
 * - Dismissing: closes the dialog with no changes.
 *
 * Note: suggestion lists ([AddEditPlayerDialogState.nameSuggestions] and
 * [AddEditPlayerDialogState.usernameSuggestions]) are populated externally by the
 * ViewModel's debounced search flows, not by this reducer.
 */
class AddEditPlayerDialogReducer @Inject constructor() {

    fun reduce(
        state: AddPlayUiState,
        event: AddPlayEvent,
    ): AddPlayUiState {
        return state.updateGameSelected {
            when (event) {

                is AddPlayEvent.AddEditPlayerDialogEvent.ShowAddPlayerDialog ->
                    copy(
                        addEditPlayerDialog = AddEditPlayerDialogState()
                    )

                is AddPlayEvent.AddEditPlayerDialogEvent.ShowEditPlayerDialog -> {
                    val existing = players.players.find {
                        it.playerIdentity == event.playerIdentity
                    }
                    copy(
                        addEditPlayerDialog = AddEditPlayerDialogState(
                            editingIdentity = event.playerIdentity,
                            name = event.playerIdentity.name,
                            username = event.playerIdentity.username.orEmpty(),
                            color = existing?.color.orEmpty(),
                        )
                    )
                }

                is AddPlayEvent.AddEditPlayerDialogEvent.DismissAddEditPlayerDialog ->
                    copy(addEditPlayerDialog = null)

                is AddPlayEvent.AddEditPlayerDialogEvent.AddEditNameChanged ->
                    copy(
                        addEditPlayerDialog = addEditPlayerDialog?.copy(
                            name = event.name,
                            nameSuggestions = emptyList(),
                        )
                    )

                is AddPlayEvent.AddEditPlayerDialogEvent.AddEditUsernameChanged ->
                    copy(
                        addEditPlayerDialog = addEditPlayerDialog?.copy(
                            username = event.username,
                            usernameSuggestions = emptyList(),
                        )
                    )

                is AddPlayEvent.AddEditPlayerDialogEvent.AddEditColorChanged ->
                    copy(
                        addEditPlayerDialog = addEditPlayerDialog?.copy(color = event.color)
                    )

                is AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer -> {
                    val dialog =
                        addEditPlayerDialog ?: return@updateGameSelected copy(addEditPlayerDialog = null)
                    val newName = dialog.name.trim()
                        .split("\\s+".toRegex())
                        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                    if (newName.isBlank()) return@updateGameSelected copy(addEditPlayerDialog = null)

                    val newIdentity = PlayerIdentity(
                        name = newName,
                        username = dialog.username.trim().ifBlank { null },
                        userId = dialog.editingIdentity?.userId,
                    )
                    val rawColor = dialog.color.trim().ifBlank { null }
                    val newColor = rawColor?.let { PlayerColor.fromString(it)?.colorString ?: it }

                    val updatedPlayers = if (dialog.editingIdentity == null) {
                        // Adding a brand-new player
                        players.players + PlayerEntryUi(
                            playerIdentity = newIdentity,
                            startPosition = players.players.size + 1,
                            color = newColor,
                            score = null,
                            isWinner = false,
                        )
                    } else {
                        // Editing an existing player
                        players.players.map { player ->
                            if (player.playerIdentity == dialog.editingIdentity) {
                                player.copy(playerIdentity = newIdentity, color = newColor)
                            } else {
                                player
                            }
                        }
                    }

                    copy(
                        players = players.copy(players = updatedPlayers),
                        addEditPlayerDialog = null,
                    )
                }

                else -> this
            }
        }
    }
}
