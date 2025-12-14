package app.meeplebook.core.sync.model

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.plays.model.PlayError

/**
 * Errors that can occur during user data synchronization.
 */
sealed class SyncUserDataError {
    data object NotLoggedIn : SyncUserDataError()
    data class CollectionSyncFailed(val error: CollectionError) : SyncUserDataError()
    data class PlaysSyncFailed(val error: PlayError) : SyncUserDataError()
}