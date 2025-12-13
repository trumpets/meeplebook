package app.meeplebook.feature.collection

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
 * Collection screen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The CollectionViewModel (injected by Hilt)
 */
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
            text = stringResource(R.string.home_collection_tab_coming_soon),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
