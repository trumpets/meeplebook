package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.feature.overview.GameHighlight

data class DomainGameHighlight(
    val id: Long,
    val gameName: String,
    val thumbnailUrl: String?,
    val highlightType: HighlightType
)

enum class HighlightType { RECENTLY_ADDED, SUGGESTED }

/**
 * Maps a [CollectionItem] to a [GameHighlight] for overview display.
 */
fun CollectionItem.toDomain(highlightType: HighlightType): DomainGameHighlight {
    return DomainGameHighlight(
        id = gameId,
        gameName = name,
        thumbnailUrl = thumbnail,
        highlightType = highlightType
    )
}