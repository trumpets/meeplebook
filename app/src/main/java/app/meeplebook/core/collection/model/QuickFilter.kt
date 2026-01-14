package app.meeplebook.core.collection.model

/**
 * Quick filters that can be applied to a user's board game collection.
 *
 * These filters are intended for common collection views, such as
 * showing all games or only games that have not yet been played.
 */
enum class QuickFilter {
    /**
     * Show all games in the collection, regardless of play status.
     */
    ALL,

    /**
     * Show only games that have no recorded plays.
     */
    UNPLAYED
}