package app.meeplebook.core.sync.model

import java.time.Instant

/**
 * App-facing sync execution state for a single sync domain.
 */
data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val errorMessage: String? = null
)
