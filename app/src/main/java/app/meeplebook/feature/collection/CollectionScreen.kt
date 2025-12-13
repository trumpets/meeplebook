package app.meeplebook.feature.collection

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
 * Collection screen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The CollectionViewModel (injected by Hilt)
 */
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectionContent(uiState = uiState)
}

@Composable
fun CollectionContent(
    uiState: CollectionUiState,
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