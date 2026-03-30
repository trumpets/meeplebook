package app.meeplebook.feature.gamedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Game Detail screen.
 *
 * Entry point: [gameId] is always provided via [SavedStateHandle] (from
 * [app.meeplebook.ui.navigation.Screen.GameDetail]).
 *
 * Data is refreshed automatically on entry and can be manually triggered via
 * [GameDetailEvent.Refresh] (pull-to-refresh).
 *
 * TODO: Wire in repository calls for collection item, plays, and derived average duration.
 */
@HiltViewModel
class GameDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val gameId: Long = checkNotNull(savedStateHandle[GAME_ID_KEY]) {
        "GameDetailViewModel requires a gameId in SavedStateHandle"
    }

    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: GameDetailEvent) {
        when (event) {
            is GameDetailEvent.Refresh -> onRefresh()
            is GameDetailEvent.PlayClicked -> { /* TODO: navigate to play detail */ }
            is GameDetailEvent.WebLinkClicked -> { /* TODO: open URL via UI effect */ }
        }
    }

    private fun onRefresh() {
        // TODO: trigger data reload from repository
    }

    companion object {
        const val GAME_ID_KEY = "gameId"
    }
}
