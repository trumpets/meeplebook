package app.meeplebook.feature.plays

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Plays screen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The PlaysViewModel (injected by Hilt)
 */
@Composable
fun PlaysScreen(
    viewModel: PlaysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlaysContent(uiState = uiState)
}

@Composable
fun PlaysContent(
    uiState: PlaysUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Coming Soon",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}