package app.meeplebook.feature.collection

import app.meeplebook.R
import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.collection.domain.DomainCollectionSection

/**
 * Maps a [DomainCollectionItem] to a [CollectionGameItem] for collection display.
 */
fun DomainCollectionItem.toCollectionGameItem(): CollectionGameItem {
    val playersSubtitleUiText = if (minPlayers == null && maxPlayers == null) {
        uiTextEmpty()
    } else {
        uiTextRes(
            R.string.collection_player_count,
            minPlayers ?: maxPlayers ?: 0,
            maxPlayers ?: minPlayers ?: 0
        )
    }

    val playTimeSubtitleUiText = if (minPlayTimeMinutes == null && maxPlayTimeMinutes == null) {
        uiTextEmpty()
    } else {
        uiTextRes(
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
        playsSubtitleUiText = uiTextRes(R.string.collection_plays_count, playCount),
        playersSubtitleUiText = playersSubtitleUiText,
        playTimeSubtitleUiText = playTimeSubtitleUiText,
        isUnplayed = playCount == 0
    )
}

/**
 * Maps a [DomainCollectionSection] to a [CollectionSection] for collection display.
 */
fun DomainCollectionSection.toCollectionSection(): CollectionSection {
    return CollectionSection(
        key = key,
        games = items.map { it.toCollectionGameItem() }
    )
}