package app.meeplebook.ui.components.screenstates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.meeplebook.core.ui.UiText
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.components.UiTextText

@Composable
fun LoadingState(
    loadingMessageUiText: UiText
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loadingIndicator"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(ScreenPadding.ContentPadding))
            UiTextText(
                text = loadingMessageUiText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}