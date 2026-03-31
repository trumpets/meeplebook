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
import app.meeplebook.feature.addplay.ui.sections.AddPlaySections
import app.meeplebook.feature.addplay.ui.sections.Comments
import app.meeplebook.feature.addplay.ui.sections.DateDuration
import app.meeplebook.feature.addplay.ui.sections.Location
import app.meeplebook.feature.addplay.ui.sections.Players
import app.meeplebook.feature.addplay.ui.sections.QuantityIncomplete
import app.meeplebook.feature.addplay.ui.sections.SuggestedPlayers

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
            AddPlaySections.Location(
                locationState = state.location,
                onEvent = onEvent
            )
        }

        item { AddPlaySections.DateDuration(date = state.date, durationMinutes = state.durationMinutes, onEvent = onEvent) }

        if (state.showQuantity || state.showIncomplete) {
            item { AddPlaySections.QuantityIncomplete(state = state, onEvent = onEvent) }
        }

        if (state.showComments) {
            item { AddPlaySections.Comments(comments = state.comments, onEvent = onEvent) }
        }

        item { AddPlaySections.SuggestedPlayers(state = state, onEvent = onEvent) }

        item { AddPlaySections.Players(state = state, onEvent = onEvent, snackbarHostState = snackbarHostState) }
    }
}