package app.meeplebook.core.database.projection

import app.meeplebook.core.plays.domain.PlayerIdentity

/**
 * Projection of player data from a play, used for displaying player information
 * in the context of a play's location.
 */
data class PlayerLocationProjection(
    val name: String,
    val username: String?,
    val userId: Long
)

/**
 * Maps a [PlayerLocationProjection] to a [PlayerIdentity] domain model.
 */
fun PlayerLocationProjection.toPlayerIdentity(): PlayerIdentity {
    return PlayerIdentity(
        name = name,
        username = username,
        userId = userId
    )
}