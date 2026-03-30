package app.meeplebook.feature.gamedetail

import app.meeplebook.core.collection.model.GameRank
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.ui.UiText

/**
 * UI state for the Game Detail screen.
 *
 * The screen always receives a [gameId] as its entry point. Data is refreshed automatically
 * on entry and can be manually triggered via pull-to-refresh. [isRefreshing] is exposed on
 * all states so the pull-to-refresh indicator is visible regardless of the current state.
 */
sealed interface GameDetailUiState {

    val isRefreshing: Boolean

    /**
     * Initial loading state shown before any data has been fetched.
     */
    data class Loading(
        override val isRefreshing: Boolean = true
    ) : GameDetailUiState

    /**
     * Content state shown once game data has been loaded successfully.
     *
     * @property gameId The BGG game ID for this screen.
     * @property name The game's display name.
     * @property imageUrl Full-size cover image URL. Preferred for the hero.
     * @property thumbnailUrl Low-resolution thumbnail URL. Fallback when [imageUrl] is null.
     * @property yearPublished The year the game was first published, or null if unknown.
     * @property ranks BGG ranking entries (subtype rank + zero or more family ranks).
     * @property minPlayTimeMinutes BGG-listed minimum play time in minutes.
     * @property maxPlayTimeMinutes BGG-listed maximum play time in minutes.
     * @property avgActualDuration Average play duration derived from the user's logged plays.
     *   Null when there are no logged plays with a recorded duration.
     * @property playCount Number of logged plays for this game (from the collection record).
     * @property userRating The user's personal BGG rating (1–10), or null if not yet rated.
     * @property webLinks Ordered list of external links shown in the detail screen.
     * @property plays The user's logged plays for this game, newest first.
     * @property isRefreshing Whether a refresh is currently in progress.
     */
    data class Content(
        val gameId: Long,
        val name: String,
        val imageUrl: String?,
        val thumbnailUrl: String?,
        val yearPublished: Int?,
        val ranks: List<GameRank>,
        val minPlayTimeMinutes: Int?,
        val maxPlayTimeMinutes: Int?,
        val avgActualDuration: AveragePlayDuration?,
        val playCount: Int,
        val userRating: Float?,
        val webLinks: List<GameWebLink>,
        val plays: List<GameDetailPlayItem>,
        override val isRefreshing: Boolean
    ) : GameDetailUiState

    /**
     * Error state shown when data could not be loaded.
     *
     * @property message Human-readable description of the error.
     * @property isRefreshing Whether a refresh is currently in progress.
     */
    data class Error(
        val message: UiText,
        override val isRefreshing: Boolean = false
    ) : GameDetailUiState
}

/**
 * Represents the average play duration derived from logged plays.
 *
 * @property playerCount The specific player count this average applies to, or null when it
 *   spans all logged player counts combined.
 * @property avgMinutes The rounded average duration in minutes.
 */
data class AveragePlayDuration(
    val playerCount: Int?,
    val avgMinutes: Int
)

/**
 * A single logged play entry shown on the Game Detail screen.
 *
 * @property playId Unique identifier for the play record.
 * @property dateUiText Formatted play date (e.g. "27/01/2026").
 * @property durationUiText Formatted duration (e.g. "120 min"), or empty when not recorded.
 * @property playerSummaryUiText Summary of players (e.g. "Ivo (winner), Maja").
 * @property location Optional location where the play took place.
 * @property comments Optional user comments for this play.
 */
data class GameDetailPlayItem(
    val playId: PlayId,
    val dateUiText: UiText,
    val durationUiText: UiText,
    val playerSummaryUiText: UiText,
    val location: String?,
    val comments: String?
)

/**
 * An external web link shown on the Game Detail screen.
 *
 * @property type The category of link, used for display label and icon selection.
 * @property url The fully-qualified URL to open.
 */
data class GameWebLink(
    val type: WebLinkType,
    val url: String
)

/**
 * Categories of external web links for a game.
 */
enum class WebLinkType {
    /** The game's main page on BoardGameGeek. */
    BGG_GAME_PAGE,

    /** The game's forum threads on BGG. */
    BGG_FORUM,

    /** Video reviews and playthroughs on BGG. */
    BGG_VIDEOS,

    /** Marketplace listings on BGG. */
    BGG_MARKETPLACE
}
