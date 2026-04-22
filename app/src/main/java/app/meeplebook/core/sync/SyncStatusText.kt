package app.meeplebook.core.sync

import app.meeplebook.R
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.formatLastSynced

/**
 * Maps persisted sync state into short, user-facing status text for screen chrome.
 */
fun SyncState.toSyncStatusUiText(type: SyncType): UiText {
    return when {
        isSyncing -> uiTextRes(R.string.sync_in_progress)
        errorMessage != null -> when (type) {
            SyncType.COLLECTION -> uiTextRes(R.string.sync_collections_failed_error)
            SyncType.PLAYS -> uiTextRes(R.string.sync_plays_failed_error)
        }

        else -> formatLastSynced(lastSyncedAt)
    }
}

/**
 * Maps the derived full-sync state into user-facing status text for Overview.
 */
fun SyncState.toFullSyncStatusUiText(): UiText {
    return when {
        isSyncing -> uiTextRes(R.string.sync_in_progress)
        errorMessage != null -> uiTextRes(R.string.sync_failed_error)
        else -> formatLastSynced(lastSyncedAt)
    }
}
