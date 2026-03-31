package app.meeplebook.feature.addplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.meeplebook.R

/**
 * Entry point composable for the "Log a Play" feature.
 *
 * The [AddPlayViewModel], state model ([AddPlayUiState]), events ([AddPlayEvent]),
 * reducers and effect-producers are already in place. This screen composable is a
 * placeholder that will be replaced with the full UI in a follow-up task.
 *
 * @param gameId   Optional BGG game ID pre-selected for this play (passed from the
 *                 Collection screen "Log play" shortcut).
 * @param gameName Optional display name for the pre-selected game.
 * @param onNavigateBack Callback invoked when the user dismisses this screen.
 */
@Composable
fun AddPlayScreen(
    gameId: Long? = null,
    gameName: String? = null,
    onNavigateBack: () -> Unit = {},
) {
    // TODO: Implement full Add Play UI — ViewModel, reducers, and effects are already in place.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.add_play_title))
    }
}
