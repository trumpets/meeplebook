package app.meeplebook.feature.addplay.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.ui.sections.CommentsSection
import app.meeplebook.feature.addplay.ui.sections.DateDurationSection
import app.meeplebook.feature.addplay.ui.sections.LocationSection
import app.meeplebook.feature.addplay.ui.sections.PlayersSection
import app.meeplebook.feature.addplay.ui.sections.QuantityIncompleteRow
import app.meeplebook.feature.addplay.ui.sections.SuggestedPlayersSection

@Composable
fun GameSelectedContent(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("addPlayForm"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            LocationSection(
                locationState = state.location,
                onEvent = onEvent
            )
        }

        item { DateDurationSection(date = state.date, durationMinutes = state.durationMinutes, onEvent = onEvent) }

        if (state.showQuantity || state.showIncomplete) {
            item { QuantityIncompleteRow(state = state, onEvent = onEvent) }
        }

        if (state.showComments) {
            item { CommentsSection(comments = state.comments, onEvent = onEvent) }
        }

        item { SuggestedPlayersSection(state = state, onEvent = onEvent) }

        item { PlayersSection(state = state, onEvent = onEvent, snackbarHostState = snackbarHostState) }
    }
}