package app.meeplebook.core.ui.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the state of a Floating Action Button (FAB) in the UI.
 *
 * @param icon The icon to display on the FAB.
 * @param contentDescription The content description for accessibility.
 * @param onClick The action to perform when the FAB is clicked.
 * @param testTag An optional test tag for UI testing.
 */
data class FabState(
    val icon: ImageVector = Icons.Default.Add,
    val contentDescription: String? = null,
    val onClick: (() -> Unit),
    val testTag: String? = null
)