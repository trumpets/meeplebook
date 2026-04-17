package app.meeplebook.feature.addplay.ui.sections

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.PLAYER_ROW_HEIGHT
import app.meeplebook.feature.addplay.ui.dialogs.AddEditPlayerDialog
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.feature.addplay.ui.components.PlayerEntryRow
import app.meeplebook.feature.addplay.ui.dialogs.ColorPickerDialog
import app.meeplebook.feature.addplay.ui.dialogs.ScoreInputDialog
import app.meeplebook.feature.addplay.ui.previewGameSelectedState
import app.meeplebook.feature.addplay.ui.previewPlayers
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPlaySections.Players(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val players = state.players.players
    val undoLabel = stringResource(R.string.undo)
    val playerRemovedMsg = stringResource(R.string.player_removed)

    var pendingUndo by remember { mutableStateOf<Pair<PlayerEntryUi, Int>?>(null) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { PLAYER_ROW_HEIGHT.toPx() }

    var scoreDialogPlayer by remember { mutableStateOf<PlayerEntryUi?>(null) }
    var colorDialogPlayer by remember { mutableStateOf<PlayerEntryUi?>(null) }

    scoreDialogPlayer?.let { target ->
        ScoreInputDialog(
            player = target,
            onConfirm = { score ->
                onEvent(
                    AddPlayEvent.PlayerScoreEvent.ScoreChanged(
                        playerIdentity = target.playerIdentity,
                        score = score,
                    )
                )
                scoreDialogPlayer = null
            },
            onDismiss = { scoreDialogPlayer = null },
        )
    }

    colorDialogPlayer?.let { target ->
        ColorPickerDialog(
            currentColor = PlayerColor.fromString(target.color),
            colorsHistory = state.players.colorsHistory,
            onColorSelected = { color ->
                onEvent(
                    AddPlayEvent.PlayerColorEvent.ColorSelected(
                        playerIdentity = target.playerIdentity,
                        color = color,
                    )
                )
                colorDialogPlayer = null
            },
            onDismiss = { colorDialogPlayer = null },
        )
    }

    state.addEditPlayerDialog?.let { dialogState ->
        AddEditPlayerDialog(
            state = dialogState,
            colorsHistory = state.players.colorsHistory,
            onNameChanged = { name ->
                onEvent(AddPlayEvent.AddEditPlayerDialogEvent.AddEditNameChanged(name))
            },
            onUsernameChanged = { username ->
                onEvent(AddPlayEvent.AddEditPlayerDialogEvent.AddEditUsernameChanged(username))
            },
            onColorChanged = { color ->
                onEvent(AddPlayEvent.AddEditPlayerDialogEvent.AddEditColorChanged(color))
            },
            onConfirm = {
                onEvent(AddPlayEvent.AddEditPlayerDialogEvent.ConfirmAddEditPlayer)
            },
            onDismiss = {
                onEvent(AddPlayEvent.AddEditPlayerDialogEvent.DismissAddEditPlayerDialog)
            },
        )
    }

    LaunchedEffect(pendingUndo) {
        pendingUndo?.let { (player, atIndex) ->
            val result = snackbarHostState.showSnackbar(
                message = playerRemovedMsg,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onEvent(AddPlayEvent.PlayerListEvent.RestorePlayer(player, atIndex))
            }
            pendingUndo = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.add_play_players_label_with_count, players.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(
                onClick = { onEvent(AddPlayEvent.AddEditPlayerDialogEvent.ShowAddPlayerDialog) },
            ) {
                Text(text = stringResource(R.string.add_play_add_player))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        players.forEachIndexed { index, player ->
            key("${player.playerIdentity.name}:${player.playerIdentity.username.orEmpty()}:$index") {
                val swipeState = rememberSwipeToDismissBoxState()

                LaunchedEffect(swipeState.currentValue) {
                    if (swipeState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                        pendingUndo = player to index
                        onEvent(AddPlayEvent.PlayerListEvent.RemovePlayer(player.playerIdentity))
                    } else if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                        onEvent(
                            AddPlayEvent.AddEditPlayerDialogEvent.ShowEditPlayerDialog(
                                playerIdentity = player.playerIdentity
                            )
                        )
                    }
                }

                // Capture current index for drag lambdas; rememberUpdatedState keeps
                // them fresh after list reorders without restarting pointerInput.
                val currentIndex by rememberUpdatedState(index)
                val currentPlayers by rememberUpdatedState(players)

                val isDragging = draggingIndex == index
                SwipeToDismissBox(
                    state = swipeState,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val isDeleteDir = swipeState.targetValue == SwipeToDismissBoxValue.StartToEnd
                        val isEditDir = swipeState.targetValue == SwipeToDismissBoxValue.EndToStart
                        val bgColor = when {
                            isDeleteDir -> MaterialTheme.colorScheme.errorContainer
                            isEditDir -> MaterialTheme.colorScheme.secondaryContainer
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgColor)
                                .padding(horizontal = 16.dp),
                        ) {
                            if (isDeleteDir) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.align(Alignment.CenterStart),
                                )
                            } else if (isEditDir) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { if (isDragging) translationY = dragOffsetY },
                ) {
                    PlayerEntryRow(
                        player = player,
                        onWinnerToggle = {
                            onEvent(
                                AddPlayEvent.PlayerScoreEvent.WinnerToggled(
                                    playerIdentity = player.playerIdentity,
                                    isWinner = !player.isWinner,
                                )
                            )
                        },
                        onScoreClick = { scoreDialogPlayer = player },
                        onColorClick = { colorDialogPlayer = player },
                        onDragStart = {
                            draggingIndex = currentIndex
                            dragOffsetY = 0f
                        },
                        onDrag = { deltaY -> dragOffsetY += deltaY },
                        onDragEnd = {
                            val rawDelta = dragOffsetY / rowHeightPx
                            val targetIndex = (draggingIndex + rawDelta.roundToInt())
                                .coerceIn(0, currentPlayers.lastIndex)
                            if (targetIndex != draggingIndex) {
                                onEvent(
                                    AddPlayEvent.PlayerListEvent.PlayerReordered(
                                        fromIndex = draggingIndex,
                                        toIndex = targetIndex,
                                    )
                                )
                            }
                            draggingIndex = -1
                            dragOffsetY = 0f
                        },
                    )
                }
            }
        }
    }
}
// ── Previews ──────────────────────────────────────────────────────────────────

private class PlayersSectionPreviewProvider :
    PreviewParameterProvider<AddPlayUiState.GameSelected> {
    override val values = sequenceOf(
        previewGameSelectedState(),
        previewGameSelectedState(
            players = previewPlayers(),
            colorsHistory = listOf(PlayerColor.BLUE, PlayerColor.RED),
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlayersSectionPreview(
    @PreviewParameter(PlayersSectionPreviewProvider::class) state: AddPlayUiState.GameSelected,
) {
    MeepleBookTheme {
        AddPlaySections.Players(state = state, onEvent = {}, snackbarHostState = SnackbarHostState())
    }
}
