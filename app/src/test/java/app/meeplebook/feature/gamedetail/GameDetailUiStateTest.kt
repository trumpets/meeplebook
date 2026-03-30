package app.meeplebook.feature.gamedetail

import app.meeplebook.core.collection.model.GameRank
import app.meeplebook.core.collection.model.RankType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetailUiStateTest {

    // --- Loading ---

    @Test
    fun `Loading defaults to isRefreshing true`() {
        val state = GameDetailUiState.Loading()
        assertTrue(state.isRefreshing)
    }

    @Test
    fun `Loading can be created with isRefreshing false`() {
        val state = GameDetailUiState.Loading(isRefreshing = false)
        assertFalse(state.isRefreshing)
    }

    // --- Content ---

    @Test
    fun `Content holds all provided fields`() {
        val ranks = listOf(
            GameRank(type = RankType.SUBTYPE, name = "boardgame", friendlyName = "Board Game Rank", value = 1),
            GameRank(type = RankType.FAMILY, name = "strategygames", friendlyName = "Strategy Game Rank", value = 5)
        )
        val plays = listOf(
            makePlayItem()
        )
        val links = listOf(
            GameWebLink(WebLinkType.BGG_GAME_PAGE, "https://boardgamegeek.com/boardgame/174430")
        )
        val avgDuration = AveragePlayDuration(playerCount = 3, avgMinutes = 95)

        val state = GameDetailUiState.Content(
            gameId = 174430L,
            name = "Gloomhaven",
            imageUrl = "https://example.com/image.jpg",
            thumbnailUrl = "https://example.com/thumb.jpg",
            yearPublished = 2017,
            ranks = ranks,
            minPlayTimeMinutes = 60,
            maxPlayTimeMinutes = 120,
            avgActualDuration = avgDuration,
            playCount = 10,
            userRating = 9f,
            webLinks = links,
            plays = plays,
            isRefreshing = false
        )

        assertEquals(174430L, state.gameId)
        assertEquals("Gloomhaven", state.name)
        assertEquals("https://example.com/image.jpg", state.imageUrl)
        assertEquals("https://example.com/thumb.jpg", state.thumbnailUrl)
        assertEquals(2017, state.yearPublished)
        assertEquals(2, state.ranks.size)
        assertEquals(RankType.SUBTYPE, state.ranks[0].type)
        assertEquals(60, state.minPlayTimeMinutes)
        assertEquals(120, state.maxPlayTimeMinutes)
        assertEquals(3, state.avgActualDuration?.playerCount)
        assertEquals(95, state.avgActualDuration?.avgMinutes)
        assertEquals(10, state.playCount)
        assertEquals(9f, state.userRating)
        assertEquals(1, state.webLinks.size)
        assertEquals(1, state.plays.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `Content with null optional fields compiles correctly`() {
        val state = GameDetailUiState.Content(
            gameId = 1L,
            name = "Minimal Game",
            imageUrl = null,
            thumbnailUrl = null,
            yearPublished = null,
            ranks = emptyList(),
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            avgActualDuration = null,
            playCount = 0,
            userRating = null,
            webLinks = emptyList(),
            plays = emptyList(),
            isRefreshing = false
        )

        assertNull(state.imageUrl)
        assertNull(state.thumbnailUrl)
        assertNull(state.yearPublished)
        assertTrue(state.ranks.isEmpty())
        assertNull(state.minPlayTimeMinutes)
        assertNull(state.maxPlayTimeMinutes)
        assertNull(state.avgActualDuration)
        assertEquals(0, state.playCount)
        assertNull(state.userRating)
        assertTrue(state.webLinks.isEmpty())
        assertTrue(state.plays.isEmpty())
    }

    @Test
    fun `Content isRefreshing true while pull-to-refresh in progress`() {
        val state = makeContentState(isRefreshing = true)
        assertTrue(state.isRefreshing)
    }

    @Test
    fun `GameDetailPlayItem holds realistic field values`() {
        val playItem = makeRealisticPlayItem()
        assertEquals("27/01/2026", (playItem.dateUiText as app.meeplebook.core.ui.UiText.Plain).value)
        assertEquals("120 min", (playItem.durationUiText as app.meeplebook.core.ui.UiText.Plain).value)
        assertEquals("Ivo (winner), Maja", (playItem.playerSummaryUiText as app.meeplebook.core.ui.UiText.Plain).value)
        assertEquals("Home", playItem.location)
        assertEquals("Great game!", playItem.comments)
    }

    // --- Error ---

    @Test
    fun `Error defaults to isRefreshing false`() {
        val state = GameDetailUiState.Error(message = app.meeplebook.core.ui.uiTextEmpty())
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `Error can show refresh indicator while retrying`() {
        val state = GameDetailUiState.Error(
            message = app.meeplebook.core.ui.uiTextEmpty(),
            isRefreshing = true
        )
        assertTrue(state.isRefreshing)
    }

    // --- AveragePlayDuration ---

    @Test
    fun `AveragePlayDuration with null playerCount represents all player counts`() {
        val avg = AveragePlayDuration(playerCount = null, avgMinutes = 75)
        assertNull(avg.playerCount)
        assertEquals(75, avg.avgMinutes)
    }

    @Test
    fun `AveragePlayDuration with specific playerCount`() {
        val avg = AveragePlayDuration(playerCount = 4, avgMinutes = 110)
        assertEquals(4, avg.playerCount)
        assertEquals(110, avg.avgMinutes)
    }

    // --- GameWebLink ---

    @Test
    fun `GameWebLink holds type and url`() {
        val link = GameWebLink(WebLinkType.BGG_GAME_PAGE, "https://boardgamegeek.com/boardgame/1")
        assertEquals(WebLinkType.BGG_GAME_PAGE, link.type)
        assertEquals("https://boardgamegeek.com/boardgame/1", link.url)
    }

    @Test
    fun `all WebLinkType values are accessible`() {
        val types = WebLinkType.entries
        assertTrue(types.contains(WebLinkType.BGG_GAME_PAGE))
        assertTrue(types.contains(WebLinkType.BGG_FORUM))
        assertTrue(types.contains(WebLinkType.BGG_VIDEOS))
        assertTrue(types.contains(WebLinkType.BGG_MARKETPLACE))
    }

    // --- Helpers ---

    private fun makeContentState(isRefreshing: Boolean = false) = GameDetailUiState.Content(
        gameId = 1L,
        name = "Game",
        imageUrl = null,
        thumbnailUrl = null,
        yearPublished = 2020,
        ranks = emptyList(),
        minPlayTimeMinutes = 30,
        maxPlayTimeMinutes = 60,
        avgActualDuration = null,
        playCount = 5,
        userRating = 7f,
        webLinks = emptyList(),
        plays = emptyList(),
        isRefreshing = isRefreshing
    )

    private fun makePlayItem(): GameDetailPlayItem {
        val playId = app.meeplebook.core.plays.model.PlayId.Local(localId = 1L)
        return GameDetailPlayItem(
            playId = playId,
            dateUiText = app.meeplebook.core.ui.uiTextEmpty(),
            durationUiText = app.meeplebook.core.ui.uiTextEmpty(),
            playerSummaryUiText = app.meeplebook.core.ui.uiTextEmpty(),
            location = null,
            comments = null
        )
    }

    private fun makeRealisticPlayItem(): GameDetailPlayItem {
        val playId = app.meeplebook.core.plays.model.PlayId.Local(localId = 42L)
        return GameDetailPlayItem(
            playId = playId,
            dateUiText = app.meeplebook.core.ui.UiText.Plain("27/01/2026"),
            durationUiText = app.meeplebook.core.ui.UiText.Plain("120 min"),
            playerSummaryUiText = app.meeplebook.core.ui.UiText.Plain("Ivo (winner), Maja"),
            location = "Home",
            comments = "Great game!"
        )
    }
}
