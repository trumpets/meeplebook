package app.meeplebook.feature.addplay.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.PLAYER_ROW_HEIGHT
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.feature.addplay.ui.dialogs.ColorPickerDialog
import app.meeplebook.feature.addplay.ui.dialogs.ScoreInputDialog
import app.meeplebook.feature.addplay.ui.components.PlayerEntryRow
import app.meeplebook.ui.components.ScreenPadding
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersSection(
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
            player = target,
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
        Text(
            text = stringResource(R.string.add_play_players_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        players.forEachIndexed { index, player ->
            key(player.playerIdentity.name + (player.playerIdentity.username ?: "")) {
                val swipeState = rememberSwipeToDismissBoxState()

                LaunchedEffect(swipeState.currentValue) {
                    if (swipeState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
                        pendingUndo = player to index
                        onEvent(AddPlayEvent.PlayerListEvent.RemovePlayer(player.playerIdentity))
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