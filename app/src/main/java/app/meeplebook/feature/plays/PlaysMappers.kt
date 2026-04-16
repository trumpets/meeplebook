package app.meeplebook.feature.plays

import app.meeplebook.R
import app.meeplebook.core.plays.domain.DomainPlayItem
import app.meeplebook.core.plays.domain.DomainPlayStatsSummary
import app.meeplebook.core.plays.domain.DomainPlayerItem
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.core.ui.uiTextJoin
import app.meeplebook.core.ui.uiTextPlural
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.ScoreFormatter
import app.meeplebook.core.util.formatDuration
import app.meeplebook.core.util.formatRelativeDate
import app.meeplebook.feature.plays.domain.DomainPlaysScreenData
import app.meeplebook.feature.plays.domain.DomainPlaysSection

/** Maps a domain play item into the render-ready [PlayItem] used by the Plays UI. */
fun DomainPlayItem.toPlayItem(): PlayItem {
    val durationUiText = if (durationMinutes != null) formatDuration(durationMinutes) else uiTextEmpty()

    return PlayItem(
        playId = playId,
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
 * Formats the player summary line shown in a play row.
 *
 * Players are sorted by `startPosition` when available. Wins and scores are appended as localized
 * details so the row can be rendered directly with [UiText].
 *
 * @param players Players belonging to a single play.
 * @return A localized summary of the players for UI display.
 */
fun formatPlayerSummary(players: List<DomainPlayerItem>): UiText {
    if (players.isEmpty()) {
        return uiTextEmpty()
    }

    val sortedPlayers = players.sortedBy { it.startPosition ?: Int.MAX_VALUE }

    val formattedPlayers = sortedPlayers.map { player ->
        val details = buildList {
            if (player.win) add(uiTextRes(R.string.play_player_won))
            player.score?.let { add(uiTextRes(R.string.play_player_score, ScoreFormatter.format(it))) }
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

    return uiTextPlural(
        R.plurals.play_players_formatted,
        formattedPlayers.size,
        formattedPlayers.size,
        uiTextJoin(", ", *formattedPlayers.toTypedArray())
    )
}

/** Maps a domain section to the UI section model used on the Plays screen. */
fun DomainPlaysSection.toPlaysSection(): PlaysSection {
    return PlaysSection(
        monthYearDate = monthYearDate,
        plays = items.map { it.toPlayItem() }
    )
}

/** Maps domain play statistics to the summary card model shown by the Plays UI. */
fun DomainPlayStatsSummary.toPlayStats(): PlayStats {
    return PlayStats(
        uniqueGamesCount = uniqueGamesCount,
        totalPlays = totalPlays,
        playsThisYear = playsThisYear,
        currentYear = currentYear
    )
}

/**
 * Converts full domain screen data plus reducer-owned [baseState] into a [PlaysUiState].
 *
 * This is the join point between the domain observer ([DomainPlaysScreenData]) and the reducer
 * pipeline ([PlaysBaseState]). The function keeps display-state derivation out of the reducer.
 */
fun DomainPlaysScreenData.toUiState(baseState: PlaysBaseState): PlaysUiState {
    val common = PlaysCommonState(
        searchQuery = baseState.searchQuery,
        playStats = stats.toPlayStats(),
        isRefreshing = baseState.isRefreshing
    )
    val sections = this.sections.map { it.toPlaysSection() }

    return if (sections.isEmpty()) {
        PlaysUiState.Empty(
            reason = emptyReasonFor(baseState.searchQuery),
            common = common
        )
    } else {
        PlaysUiState.Content(
            sections = sections,
            common = common
        )
    }
}

/** Derives the appropriate empty-state reason from the current search query. */
private fun emptyReasonFor(searchQuery: String): EmptyReason {
    return if (searchQuery.trim().isNotEmpty()) {
        EmptyReason.NO_SEARCH_RESULTS
    } else {
        EmptyReason.NO_PLAYS
    }
}
