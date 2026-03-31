package app.meeplebook.feature.addplay.ui

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.LocationState
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.feature.addplay.PlayerSuggestion
import app.meeplebook.feature.addplay.PlayersState
import app.meeplebook.feature.addplay.SearchResultGameItem
import java.time.Instant

/**
 * Shared preview state builders for AddPlay composable previews.
 * Internal so they are accessible across the `feature.addplay.ui` package family
 * but not exported as public API.
 */

internal fun previewGameSearchState(
    query: String = "",
    hasResults: Boolean = false,
) = AddPlayUiState.GameSearch(
    gameSearchQuery = query,
    gameSearchResults = if (hasResults) listOf(
        SearchResultGameItem(1L, "Catan", 1995, null),
        SearchResultGameItem(2L, "Ticket to Ride", 2004, null),
        SearchResultGameItem(3L, "Pandemic", 2008, null),
    ) else emptyList(),
)

internal fun previewLocationState(
    value: String? = null,
    recentLocations: List<String> = emptyList(),
    suggestions: List<String> = emptyList()
) = LocationState(
    value = value,
    suggestions = suggestions,
    recentLocations = recentLocations,
)

internal fun previewGameSelectedState(
    players: List<PlayerEntryUi> = emptyList(),
    suggestions: List<PlayerSuggestion> = emptyList(),
    colorsHistory: List<PlayerColor> = emptyList(),
    showQuantity: Boolean = false,
    showIncomplete: Boolean = false,
    showComments: Boolean = false,
    isSaving: Boolean = false,
) = AddPlayUiState.GameSelected(
    gameId = 13L,
    gameName = "Catan",
    date = Instant.parse("2026-03-30T18:00:00Z"),
    durationMinutes = 90,
    location = previewLocationState(
        value = "Home",
        recentLocations = listOf("Home", "Game Café", "Bob's place"),
    ),
    players = PlayersState(players = players, colorsHistory = colorsHistory),
    playersByLocation = suggestions,
    showQuantity = showQuantity,
    showIncomplete = showIncomplete,
    showComments = showComments,
    isSaving = isSaving,
)

internal fun previewPlayers() = listOf(
    PlayerEntryUi(
        playerIdentity = PlayerIdentity("Alice", username = "alicebgg", userId = null),
        startPosition = 1,
        color = PlayerColor.BLUE.colorString,
        score = 42.0,
        isWinner = true,
    ),
    PlayerEntryUi(
        playerIdentity = PlayerIdentity("Bob", username = null, userId = null),
        startPosition = 2,
        color = PlayerColor.RED.colorString,
        score = 38.0,
        isWinner = false,
    ),
    PlayerEntryUi(
        playerIdentity = PlayerIdentity("Charlie", username = null, userId = null),
        startPosition = 3,
        color = null,
        score = null,
        isWinner = false,
    ),
)

internal fun previewSuggestions(count: Int = 4) = (1..count).map { i ->
    PlayerSuggestion(PlayerIdentity("Player $i", username = null, userId = null))
}
