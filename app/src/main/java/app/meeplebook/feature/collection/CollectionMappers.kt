package app.meeplebook.feature.collection

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.ui.StringProvider
import app.meeplebook.feature.collection.domain.DomainCollectionSection

/**
 * Maps a [DomainCollectionItem] to a [CollectionGameItem] for collection display.
 */
fun DomainCollectionItem.toCollectionGameItem(stringProvider: StringProvider): CollectionGameItem {
    return CollectionGameItem(
        gameId = gameId,
        name = name,
        yearPublished = yearPublished,
        thumbnailUrl = thumbnailUrl,
        playsSubtitle = stringProvider.get(R.string.collection_plays_count, playCount),
        playersSubtitle = stringProvider.get(R.string.collection_player_count, minPlayers ?: 0, maxPlayers ?: 0),
        playTimeSubtitle = stringProvider.get(R.string.collection_play_time, minPlayTimeMinutes ?: 0, maxPlayTimeMinutes ?: 0),
        isNew = playCount == 0
    )
}

/**
 * Maps a [DomainCollectionSection] to a [CollectionSection] for collection display.
 */
fun DomainCollectionSection.toCollectionSection(stringProvider: StringProvider): CollectionSection {
    return CollectionSection(
        key = key,
        games = items.map { it.toCollectionGameItem(stringProvider) }
    )
}