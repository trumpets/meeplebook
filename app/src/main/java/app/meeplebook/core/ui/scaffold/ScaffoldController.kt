package app.meeplebook.core.ui.scaffold

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for managing scaffold-related UI elements such as snackbars and FABs.
 */
interface ScaffoldController {
    val snackbarHostState: SnackbarHostState
    val fabState: StateFlow<FabState?>

    suspend fun showSnackbar(message: String)

    fun setFab(state: FabState)
    fun clearFab()
}