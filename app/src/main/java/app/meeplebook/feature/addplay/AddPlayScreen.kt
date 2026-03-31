package app.meeplebook.feature.addplay

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.ui.asString
import app.meeplebook.feature.addplay.effect.AddPlayUiEffect
import app.meeplebook.feature.addplay.ui.GameSearchContent
import app.meeplebook.feature.addplay.ui.GameSelectedContent
import app.meeplebook.feature.addplay.ui.components.AddFieldFab
import app.meeplebook.feature.addplay.ui.previewGameSearchState
import app.meeplebook.feature.addplay.ui.previewGameSelectedState
import app.meeplebook.feature.addplay.ui.previewPlayers
import app.meeplebook.feature.addplay.ui.previewSuggestions
import app.meeplebook.ui.theme.MeepleBookTheme

val PLAYER_ROW_HEIGHT = 72.dp

@Composable
fun AddPlayScreen(
    preselectedGame: PreselectedGame? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddPlayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Path 2 entry: if we already have a game, select it immediately.
    LaunchedEffect(preselectedGame) {
        if (preselectedGame != null) {
            viewModel.onEvent(
                AddPlayEvent.GameSearchEvent.GameSelected(
                    gameId = preselectedGame.gameId,
                    gameName = preselectedGame.gameName
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                AddPlayUiEffect.NavigateBack -> onNavigateBack()
                is AddPlayUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.asString(resources))
                }
            }
        }
    }

    AddPlayScreenRoot(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = { viewModel.onEvent(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayScreenRoot(
    uiState: AddPlayUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onEvent: (AddPlayEvent) -> Unit
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = uiState is AddPlayUiState.GameSelected

    val onBackPressed: () -> Unit = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onEvent(AddPlayEvent.ActionEvent.CancelClicked)
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        onBackPressed()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.add_play_discard_title)) },
            text = { Text(stringResource(R.string.add_play_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onEvent(AddPlayEvent.ActionEvent.CancelClicked)
                    }
                ) {
                    Text(stringResource(R.string.add_play_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.add_play_discard_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState) {
                            is AddPlayUiState.GameSelected -> uiState.gameName
                            is AddPlayUiState.GameSearch -> stringResource(R.string.add_play_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.add_play_cancel)
                        )
                    }
                },
                actions = {
                    if (uiState is AddPlayUiState.GameSelected) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(
                                onClick = { onEvent(AddPlayEvent.ActionEvent.SaveClicked) },
                                enabled = uiState.canSave
                            ) {
                                Text(stringResource(R.string.add_play_save))
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is AddPlayUiState.GameSelected) {
                val hiddenFields = buildList {
                    if (!uiState.showQuantity) add(OptionalField.QUANTITY)
                    if (!uiState.showIncomplete) add(OptionalField.INCOMPLETE)
                    if (!uiState.showComments) add(OptionalField.COMMENTS)
                }
                if (hiddenFields.isNotEmpty()) {
                    AddFieldFab(hiddenFields = hiddenFields, onEvent = onEvent)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("addPlayScreen")
        ) {
            when (uiState) {
                is AddPlayUiState.GameSearch -> GameSearchContent(
                    state = uiState,
                    onEvent = onEvent
                )

                is AddPlayUiState.GameSelected -> GameSelectedContent(
                    state = uiState,
                    onEvent = onEvent,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

class AddPlayUiStatePreviewProvider : PreviewParameterProvider<AddPlayUiState> {
    override val values: Sequence<AddPlayUiState> = sequenceOf(
        previewGameSearchState(),
        previewGameSearchState(query = "Cat", hasResults = true),
        previewGameSelectedState(),
        previewGameSelectedState(isSaving = true),
        previewGameSelectedState(players = previewPlayers(), suggestions = previewSuggestions(11)),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddPlayScreenPreview(
    @PreviewParameter(AddPlayUiStatePreviewProvider::class) previewState: AddPlayUiState
) {
    MeepleBookTheme {
        AddPlayScreenRoot(
            uiState = previewState,
            onEvent = {}
        )
    }
}
