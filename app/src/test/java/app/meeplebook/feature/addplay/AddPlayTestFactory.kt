package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import java.time.Instant

/**
 * Factory helpers for constructing AddPlay test state without boilerplate.
 */
object AddPlayTestFactory {

    fun makeIdentity(
        name: String = "Player",
        username: String? = null,
        userId: Long? = null
    ): PlayerIdentity = PlayerIdentity(name = name, username = username, userId = userId)

    fun makePlayer(
        identity: PlayerIdentity = makeIdentity(),
        startPosition: Int = 1,
        score: Double? = null,
        isWinner: Boolean = false,
        color: String? = null
    ): PlayerEntryUi = PlayerEntryUi(
        playerIdentity = identity,
        startPosition = startPosition,
        color = color,
        score = score,
        isWinner = isWinner
    )

    /**
     * Creates a [AddPlayUiState.GameSelected] for tests that need a game already selected.
     * This is the most common test state — covers all reducer/effect/viewmodel tests that
     * work with the play form (date, duration, players, saving, etc.).
     */
    fun makeGameSelectedState(
        players: List<PlayerEntryUi> = emptyList(),
        date: Instant = Instant.parse("2024-06-01T10:00:00Z"),
        durationMinutes: Int? = null,
        locationValue: String? = "",
        gameId: Long = 1L,
        gameName: String = "Test Game",
        isSaving: Boolean = false,
    ): AddPlayUiState.GameSelected = AddPlayUiState.GameSelected(
        gameId = gameId,
        gameName = gameName,
        date = date,
        durationMinutes = durationMinutes,
        location = LocationState(
            value = locationValue,
            suggestions = emptyList(),
            recentLocations = emptyList(),
            isFocused = false
        ),
        players = PlayersState(
            players = players,
            colorsHistory = PlayerColor.entries.map { it.colorString }
        ),
        playersByLocation = emptyList(),
        isSaving = isSaving
    )

    /**
     * Creates a [AddPlayUiState.GameSearch] for tests that start before a game is selected.
     */
    fun makeGameSearchState(
        gameId: Long? = null,
        gameName: String? = null,
        gameSearchQuery: String = "",
        gameSearchResults: List<SearchResultGameItem> = emptyList(),
    ): AddPlayUiState.GameSearch = AddPlayUiState.GameSearch(
        gameId = gameId,
        gameName = gameName,
        gameSearchQuery = gameSearchQuery,
        gameSearchResults = gameSearchResults,
    )
}

/**
 * Asserts that this state is [AddPlayUiState.GameSelected] and returns it.
 * Throws with a descriptive message if the cast fails — intended for test assertions only.
 */
fun AddPlayUiState.requireGameSelected(): AddPlayUiState.GameSelected =
    this as? AddPlayUiState.GameSelected
        ?: error("Expected AddPlayUiState.GameSelected but was ${this::class.simpleName}")

/**
 * Asserts that this state is [AddPlayUiState.GameSearch] and returns it.
 * Throws with a descriptive message if the cast fails — intended for test assertions only.
 */
fun AddPlayUiState.requireGameSearch(): AddPlayUiState.GameSearch =
    this as? AddPlayUiState.GameSearch
        ?: error("Expected AddPlayUiState.GameSearch but was ${this::class.simpleName}")
