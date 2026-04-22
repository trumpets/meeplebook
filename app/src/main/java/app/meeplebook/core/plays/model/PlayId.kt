package app.meeplebook.core.plays.model

import app.meeplebook.core.plays.model.PlayId.Remote

/**
 * Unique identifier for a play record.
 *
 * This sealed interface models the two identifier shapes used across the app:
 * - [Local]: a play that exists only locally and is identified by a local DB id.
 * - [Remote]: a play that has been synced with a remote service and therefore
 *   exposes both the local database id and the remote id.
 *
 * The [localId] property is always present and refers to the primary key used
 * in Room. Use [Remote.remoteId] when you need to reference the external
 * system's identifier (for example when syncing or deleting remote plays).
 */
sealed interface PlayId {

    /** The primary key id used by the local database for this play. */
    val localId: Long

    /**
     * Returns the remote id when this identifier is [Remote], or `null` for [Local].
     */
    fun remoteIdOrNull(): Long? = (this as? Remote)?.remoteId

    /**
     * Identifier for a local-only play (not yet synced).
     *
     * @property localId The local database id.
     */
    data class Local(
        override val localId: Long
    ) : PlayId

    /**
     * Identifier for a play that has been synced to the remote service.
     *
     * @property localId The local database id.
     * @property remoteId The id assigned by the remote service (BGG).
     */
    data class Remote(
        override val localId: Long,
        val remoteId: Long
    ) : PlayId
}

/**
 * Executes [block] with the remote id when this [PlayId] is [Remote].
 *
 * No-op for [PlayId.Local].
 */
inline fun PlayId.onRemote(block: (Long) -> Unit) {
    if (this is Remote) block(remoteId)
}