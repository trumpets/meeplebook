package app.meeplebook.feature.addplay

import app.meeplebook.core.collection.domain.DomainCollectionItem

/**
 * Maps a [DomainCollectionItem] to a [SearchResultGameItem] for when searching games for a play.
 */
fun DomainCollectionItem.toSearchResultGameItem(): SearchResultGameItem {
    return SearchResultGameItem(
        gameId = gameId,
        name = name,
        yearPublished = yearPublished,
        thumbnailUrl = thumbnailUrl
    )
}