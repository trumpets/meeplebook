package app.meeplebook.feature.addplay.ui.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.ui.components.ScreenPadding

@Composable
fun CommentsSection(
    comments: String,
    onEvent: (AddPlayEvent) -> Unit
) {
    OutlinedTextField(
        value = comments,
        onValueChange = { onEvent(AddPlayEvent.MetadataEvent.CommentsChanged(it)) },
        label = { Text(stringResource(R.string.add_play_comments_label)) },
        placeholder = { Text(stringResource(R.string.add_play_comments_placeholder)) },
        minLines = 1,
        maxLines = 5,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small)
            .testTag("commentsField")
    )
}