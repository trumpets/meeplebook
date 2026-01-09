package app.meeplebook.feature.collection

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.ui.StringProvider
import app.meeplebook.feature.collection.domain.DomainCollectionSection

/**
 * Maps a [DomainCollectionItem] to a [CollectionGameItem] for collection display.
 */
fun DomainCollectionItem.toCollectionGameItem(stringProvider: StringProvider): CollectionGameItem {
    val playersSubtitle = if (minPlayers == null && maxPlayers == null) {
        ""
    } else {
        stringProvider.get(
            R.string.collection_player_count,
            minPlayers ?: maxPlayers ?: 0,
            maxPlayers ?: minPlayers ?: 0
        )
    }

    val playTimeSubtitle = if (minPlayTimeMinutes == null && maxPlayTimeMinutes == null) {
        ""
    } else {
        stringProvider.get(
            R.string.collection_play_time,
            minPlayTimeMinutes ?: maxPlayTimeMinutes ?: 0,
            maxPlayTimeMinutes ?: minPlayTimeMinutes ?: 0
        )
    }

    return CollectionGameItem(
        gameId = gameId,
        name = name,
        yearPublished = yearPublished,
        thumbnailUrl = thumbnailUrl,
        playsSubtitle = stringProvider.get(R.string.collection_plays_count, playCount),
        playersSubtitle = playersSubtitle,
        playTimeSubtitle = playTimeSubtitle,
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