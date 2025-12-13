package app.meeplebook.feature.plays

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import app.meeplebook.R

/**
 * Plays screen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The PlaysViewModel (injected by Hilt)
 */
@Composable
fun PlaysScreen(
    viewModel: PlaysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
            text = stringResource(R.string.home_plays_tab_coming_soon),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
