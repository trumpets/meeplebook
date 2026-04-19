package app.meeplebook.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.meeplebook.core.sync.model.SyncType
import java.time.Instant

/**
 * Persisted sync execution state for a single sync domain.
 *
 * A singleton row exists per [type], letting the app observe current progress, last successful
 * completion time, and the latest stored error for that sync domain.
 */
@Entity(tableName = "sync_states")
data class SyncStateEntity(
    @PrimaryKey val type: SyncType,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Instant? = null,
    val errorMessage: String? = null
)
