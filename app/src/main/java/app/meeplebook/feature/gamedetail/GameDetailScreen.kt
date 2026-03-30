package app.meeplebook.feature.gamedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Game Detail screen entry point.
 *
 * Displays hero image, name, ranks, play stats, user rating, web links, and logged plays
 * for a single game. Data is refreshed automatically on entry; pull-to-refresh triggers
 * a manual reload.
 *
 * TODO: Implement full UI once repository wiring is complete.
 */
@Composable
fun GameDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GameDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
internal fun GameDetailScreen(
    uiState: GameDetailUiState,
    onEvent: (GameDetailEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    // TODO: Replace with full implementation
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Game Detail — coming soon")
    }
}
