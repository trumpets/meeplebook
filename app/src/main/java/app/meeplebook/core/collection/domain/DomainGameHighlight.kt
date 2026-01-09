package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.model.CollectionItem

data class DomainGameHighlight(
    val id: Long,
    val gameName: String,
    val thumbnailUrl: String?,
    val highlightType: HighlightType
)

enum class HighlightType { RECENTLY_ADDED, SUGGESTED }

/**
 * Maps a [CollectionItem] to a [DomainGameHighlight] for overview display.
 */
fun CollectionItem.toDomainGameHighlight(highlightType: HighlightType): DomainGameHighlight {
    return DomainGameHighlight(
        id = gameId,
        gameName = name,
        thumbnailUrl = thumbnail,
        highlightType = highlightType
    )
}