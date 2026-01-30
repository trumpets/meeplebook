package app.meeplebook.feature.plays

import app.meeplebook.R
import app.meeplebook.core.plays.domain.DomainPlayItem
import app.meeplebook.core.plays.domain.DomainPlayStatsSummary
import app.meeplebook.core.plays.domain.DomainPlayerItem
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.core.ui.uiTextJoin
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.formatDuration
import app.meeplebook.core.util.formatRelativeDate
import app.meeplebook.feature.plays.domain.DomainPlaysSection

/**
 * Maps a [DomainPlayItem] to a [PlayItem] for plays display.
 */
fun DomainPlayItem.toPlayItem(): PlayItem {
    val durationUiText = if (durationMinutes != null) formatDuration(durationMinutes) else uiTextEmpty()

    return PlayItem(
        id = id,
        gameName = gameName,
        thumbnailUrl = thumbnailUrl,
        dateUiText = formatRelativeDate(date),
        durationUiText = durationUiText,
        playerSummaryUiText = formatPlayerSummary(players),
        location = location,
        comments = comments,
        syncStatus = syncStatus
    )
}

/**
 * Formats a summary of players for a play, including their names, wins, and scores.
 *
 * @param players the list of players in the play
 * @return a UiText representing the formatted player summary
 */
fun formatPlayerSummary(players: List<DomainPlayerItem>): UiText {
    val sortedPlayers = players.sortedBy { it.startPosition?.toIntOrNull() ?: Int.MAX_VALUE }

    val formattedPlayers = sortedPlayers.map { player ->
        val details = buildList {
            if (player.win) add(uiTextRes(R.string.play_player_won))
            player.score?.let { add(uiTextRes(R.string.play_player_score, it)) }
        }

        if (details.isEmpty()) {
            uiTextRes(R.string.play_player_name_only, player.name)
        } else {
            uiTextRes(
                R.string.play_player_with_details,
                player.name,
                uiTextJoin(", ", *details.toTypedArray())
            )
        }
    }

    return uiTextRes(
        R.string.play_players_formatted,
        formattedPlayers.size,
        uiTextJoin(", ", *formattedPlayers.toTypedArray())
    )
}

/**
 * Maps a [DomainPlaysSection] to a [PlaysSection] for plays display.
 */
fun DomainPlaysSection.toPlaysSection(): PlaysSection {
    return PlaysSection(
        monthYearDate = monthYearDate,
        plays = items.map { it.toPlayItem() }
    )
}

/**
 * Maps a [DomainPlayStatsSummary] to a [PlayStats] for plays statistics display.
 */
fun DomainPlayStatsSummary.toPlayStats(): PlayStats {
    return PlayStats(
        uniqueGamesCount = uniqueGamesCount,
        totalPlays = totalPlays,
        playsThisYear = playsThisYear,
        currentYear = currentYear
    )
}