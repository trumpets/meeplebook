package app.meeplebook.feature.addplay

import kotlinx.serialization.Serializable

@Serializable
data class PreselectedGame(
    val gameId: Long,
    val gameName: String
)