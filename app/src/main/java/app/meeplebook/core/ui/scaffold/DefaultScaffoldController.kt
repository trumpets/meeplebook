package app.meeplebook.core.ui.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of [ScaffoldController] for managing FAB state and snackbars.
 *
 * @param snackbarHostState The SnackbarHostState to show snackbars
 */
@Stable
class DefaultScaffoldController(
    override val snackbarHostState: SnackbarHostState
) : ScaffoldController {

    private val _fabState = MutableStateFlow<FabState?>(null)
    override val fabState: StateFlow<FabState?> = _fabState.asStateFlow()

    override fun setFab(state: FabState) {
        _fabState.value = state
    }

    override fun clearFab() {
        _fabState.value = null
    }

    override suspend fun showSnackbar(message: String) {
        snackbarHostState.showSnackbar(message = message)
    }
}