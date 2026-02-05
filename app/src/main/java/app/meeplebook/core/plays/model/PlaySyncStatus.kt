package app.meeplebook.core.plays.model

/**
 * Synchronization status of a play record with BGG.
 */
enum class PlaySyncStatus {
    /** Play has been successfully synced with BGG. */
    SYNCED,

    /** Play is waiting to be synced with BGG. */
    PENDING,

    /** Sync attempt failed; will retry later. */
    FAILED
}