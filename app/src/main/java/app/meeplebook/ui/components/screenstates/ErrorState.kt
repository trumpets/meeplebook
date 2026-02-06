package app.meeplebook.ui.components.screenstates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.meeplebook.core.ui.UiText
import app.meeplebook.ui.components.UiTextText

@Composable
fun ErrorState(
    errorMessageUiText: UiText
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("errorState"),
        contentAlignment = Alignment.Center
    ) {
        UiTextText(
            text = errorMessageUiText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}