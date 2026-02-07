package app.meeplebook.feature.overview

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.R
import app.meeplebook.core.ui.uiText
import app.meeplebook.testutils.stringRes
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Compose tests for [OverviewContent].
 * Tests UI rendering and interaction behavior for different overview screen states.
 */
@RunWith(AndroidJUnit4::class)
class OverviewContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overviewContent_statsCard_displaysCorrectValues() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        stats = OverviewStats(
                            gamesCount = 127,
                            totalPlays = 342,
                            playsThisMonth = 18,
                            unplayedCount = 23
                        ),
                        lastSyncedUiText = uiText("Last synced: 5 min ago")
                    )
                )
            }
        }

        // Verify stats card is displayed
        composeTestRule.onNodeWithTag("overviewStatsCard").assertIsDisplayed()

        // Verify stat values are displayed
        composeTestRule.onNodeWithText("127").assertIsDisplayed()
        composeTestRule.onNodeWithText("342").assertIsDisplayed()
        composeTestRule.onNodeWithText("18").assertIsDisplayed()
        composeTestRule.onNodeWithText("23").assertIsDisplayed()

        // Verify sync text is displayed
        composeTestRule.onNodeWithText("Last synced: 5 min ago").assertIsDisplayed()
    }

    @Test
    fun overviewContent_recentPlays_rendersCorrectly() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        recentPlays = listOf(
                            RecentPlay(
                                id = 1,
                                gameName = "Catan",
                                thumbnailUrl = null,
                                dateUiText = uiText("Today, 8:30 PM"),
                                playerCount = 4,
                                playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                            ),
                            RecentPlay(
                                id = 2,
                                gameName = "Wingspan",
                                thumbnailUrl = null,
                                dateUiText = uiText("Yesterday"),
                                playerCount = 2,
                                playerNamesUiText = uiText("You, Chris")
                            )
                        )
                    )
                )
            }
        }

        // Verify recent plays are displayed
        composeTestRule.onNodeWithTag("recentPlayCard_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recentPlayCard_2").assertIsDisplayed()

        // Verify game names are displayed
        composeTestRule.onNodeWithText("Catan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wingspan").assertIsDisplayed()
    }

    @Test
    fun overviewContent_recentPlayClick_triggersCallback() {
        var clickedPlayId: Long? = null

        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        recentPlays = listOf(
                            RecentPlay(
                                id = 42,
                                gameName = "Test Game",
                                thumbnailUrl = null,
                                dateUiText = uiText("Today"),
                                playerCount = 2,
                                playerNamesUiText = uiText("You, Player")
                            )
                        )
                    ),
                    onRecentPlayClick = { clickedPlayId = it.id }
                )
            }
        }

        // Click on the recent play card
        composeTestRule.onNodeWithTag("recentPlayCard_42").performClick()

        // Verify callback was triggered with correct ID
        assertEquals(42L, clickedPlayId)
    }

    @Test
    fun overviewContent_fabClick_triggersCallback() {
        var fabClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(),
                    onLogPlayClick = { fabClicked = true }
                )
            }
        }

        // Click FAB
        composeTestRule.onNodeWithTag("logPlayFab").performClick()

        // Verify callback was triggered
        assertTrue(fabClicked)
    }

    @Test
    fun overviewContent_gameHighlightCards_displayWhenPresent() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        recentlyAddedGame = GameHighlight(
                            id = 100,
                            gameName = "Azul",
                            thumbnailUrl = null,
                            subtitleUiText = uiText("Added 2 days ago")
                        ),
                        suggestedGame = GameHighlight(
                            id = 101,
                            gameName = "Ticket to Ride",
                            thumbnailUrl = null,
                            subtitleUiText = uiText("Try Tonight?")
                        )
                    )
                )
            }
        }

        // Verify highlight cards are displayed
        composeTestRule.onNodeWithTag("highlightCard_100").assertIsDisplayed()
        composeTestRule.onNodeWithTag("highlightCard_101").assertIsDisplayed()

        // Verify game names are displayed
        composeTestRule.onNodeWithText("Azul").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ticket to Ride").assertIsDisplayed()
    }

    @Test
    fun overviewContent_emptyState_displaysZeroStats() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        stats = OverviewStats(),
                        lastSyncedUiText = uiText("Never synced")
                    )
                )
            }
        }

        // Verify zero stats are displayed
        composeTestRule.onAllNodesWithText("0").assertCountEquals(4)
        composeTestRule.onAllNodesWithText("0")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0")[1].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0")[2].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0")[3].assertIsDisplayed()

        composeTestRule.onNodeWithText("Never synced").assertIsDisplayed()
    }

    @Test
    fun overviewContent_highlightCardClick_triggersCallback() {
        var recentlyAddedClicked = false
        var suggestedClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        recentlyAddedGame = GameHighlight(
                            id = 100,
                            gameName = "Azul",
                            thumbnailUrl = null,
                            subtitleUiText = uiText("Added 2 days ago")
                        ),
                        suggestedGame = GameHighlight(
                            id = 101,
                            gameName = "Ticket to Ride",
                            thumbnailUrl = null,
                            subtitleUiText = uiText("Try Tonight?")
                        )
                    ),
                    onRecentlyAddedClick = { recentlyAddedClicked = true },
                    onSuggestedGameClick = { suggestedClicked = true }
                )
            }
        }

        // Click on recently added card
        composeTestRule.onNodeWithTag("highlightCard_100").performClick()
        assertTrue(recentlyAddedClicked)

        // Click on suggested card
        composeTestRule.onNodeWithTag("highlightCard_101").performClick()
        assertTrue(suggestedClicked)
    }

    @Test
    fun overviewContent_loadingState_displaysLoadingIndicator() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(isLoading = true)
                )
            }
        }

        // Verify loading indicator is displayed
        composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithText(stringRes(R.string.loading_message)).assertIsDisplayed()

        // Verify overview content is not displayed
        composeTestRule.onNodeWithTag("overviewContent").assertDoesNotExist()
    }

    @Test
    fun overviewContent_emptyRecentPlays_displaysEmptyStateMessage() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        stats = OverviewStats(),
                        recentPlays = emptyList()
                    )
                )
            }
        }

        // Verify empty state message is displayed
        composeTestRule.onNodeWithTag("emptyRecentPlays").assertIsDisplayed()
        composeTestRule.onNodeWithText(stringRes(R.string.empty_recent_plays_message)).assertIsDisplayed()
    }

    @Test
    fun overviewContent_refreshingState_displaysCorrectly() {
        composeTestRule.setContent {
            MeepleBookTheme {
                OverviewContent(
                    uiState = OverviewUiState(
                        isRefreshing = true,
                        recentPlays = listOf(
                            RecentPlay(
                                id = 1,
                                gameName = "Catan",
                                thumbnailUrl = null,
                                dateUiText = uiText("Today"),
                                playerCount = 4,
                                playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                            )
                        )
                    )
                )
            }
        }

        // Verify content is still displayed during refresh
        composeTestRule.onNodeWithTag("overviewContent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Catan").assertIsDisplayed()
        
        // Note: PullToRefreshBox's refresh indicator is an internal implementation detail
        // and doesn't expose test tags. The isRefreshing state is properly passed to the component.
    }
}
