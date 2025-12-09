package app.meeplebook.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Compose tests for [HomeScreenContent].
 * Tests UI rendering and interaction behavior for different home screen states.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_statsCard_displaysCorrectValues() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        stats = HomeStats(
                            gamesCount = 127,
                            totalPlays = 342,
                            playsThisMonth = 18,
                            unplayedCount = 23
                        ),
                        lastSyncedText = "Last synced: 5 min ago"
                    )
                )
            }
        }

        // Verify stats card is displayed
        composeTestRule.onNodeWithTag("statsCard").assertIsDisplayed()

        // Verify stat values are displayed
        composeTestRule.onNodeWithText("127").assertIsDisplayed()
        composeTestRule.onNodeWithText("342").assertIsDisplayed()
        composeTestRule.onNodeWithText("18").assertIsDisplayed()
        composeTestRule.onNodeWithText("23").assertIsDisplayed()

        // Verify sync text is displayed
        composeTestRule.onNodeWithText("Last synced: 5 min ago").assertIsDisplayed()
    }

    @Test
    fun homeScreen_recentPlays_rendersCorrectly() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        recentPlays = listOf(
                            RecentPlay(
                                id = 1,
                                gameName = "Catan",
                                thumbnailUrl = null,
                                dateText = "Today, 8:30 PM",
                                playerCount = 4,
                                playerNames = "You, Alex, Jordan, Sam"
                            ),
                            RecentPlay(
                                id = 2,
                                gameName = "Wingspan",
                                thumbnailUrl = null,
                                dateText = "Yesterday",
                                playerCount = 2,
                                playerNames = "You, Chris"
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
    fun homeScreen_recentPlayClick_triggersCallback() {
        var clickedPlayId: Long? = null

        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        recentPlays = listOf(
                            RecentPlay(
                                id = 42,
                                gameName = "Test Game",
                                thumbnailUrl = null,
                                dateText = "Today",
                                playerCount = 2,
                                playerNames = "You, Player"
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
    fun homeScreen_fabClick_triggersCallback() {
        var fabClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(),
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
    fun homeScreen_navigationBar_displaysAllItems() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(uiState = HomeUiState())
            }
        }

        // Verify all navigation items are displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Collection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plays").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun homeScreen_navigationBarClick_triggersCallback() {
        var selectedDestination: HomeNavigationDestination? = null

        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(),
                    onNavItemClick = { selectedDestination = it }
                )
            }
        }

        // Click on Collection nav item
        composeTestRule.onNodeWithText("Collection").performClick()

        // Verify callback was triggered with correct destination
        assertEquals(HomeNavigationDestination.COLLECTION, selectedDestination)
    }

    @Test
    fun homeScreen_gameHighlightCards_displayWhenPresent() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        recentlyAddedGame = GameHighlight(
                            id = 100,
                            gameName = "Azul",
                            thumbnailUrl = null,
                            subtitle = "Added 2 days ago"
                        ),
                        suggestedGame = GameHighlight(
                            id = 101,
                            gameName = "Ticket to Ride",
                            thumbnailUrl = null,
                            subtitle = "Try Tonight?"
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
    fun homeScreen_emptyState_displaysZeroStats() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        stats = HomeStats(),
                        lastSyncedText = "Never synced"
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
    fun homeScreen_highlightCardClick_triggersCallback() {
        var recentlyAddedClicked = false
        var suggestedClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        recentlyAddedGame = GameHighlight(
                            id = 100,
                            gameName = "Azul",
                            thumbnailUrl = null,
                            subtitle = "Added 2 days ago"
                        ),
                        suggestedGame = GameHighlight(
                            id = 101,
                            gameName = "Ticket to Ride",
                            thumbnailUrl = null,
                            subtitle = "Try Tonight?"
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
    fun homeScreen_loadingState_displaysLoadingIndicator() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(isLoading = true)
                )
            }
        }

        // Verify loading indicator is displayed
        composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading your games…").assertIsDisplayed()

        // Verify home content is not displayed
        composeTestRule.onNodeWithTag("homeContent").assertDoesNotExist()
    }

    @Test
    fun homeScreen_emptyRecentPlays_displaysEmptyStateMessage() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        stats = HomeStats(),
                        recentPlays = emptyList()
                    )
                )
            }
        }

        // Verify empty state message is displayed
        composeTestRule.onNodeWithTag("emptyRecentPlays").assertIsDisplayed()
        composeTestRule.onNodeWithText("No recent plays. Start logging your game sessions!").assertIsDisplayed()
    }

    @Test
    fun homeScreen_profileButtonClick_triggersCallback() {
        var profileClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(),
                    onProfileClick = { profileClicked = true }
                )
            }
        }

        // Click profile button
        composeTestRule.onNodeWithTag("profileButton").performClick()

        // Verify callback was triggered
        assertTrue(profileClicked)
    }

    @Test
    fun homeScreen_moreButtonClick_triggersCallback() {
        var moreClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(),
                    onMoreClick = { moreClicked = true }
                )
            }
        }

        // Click more button
        composeTestRule.onNodeWithTag("moreButton").performClick()

        // Verify callback was triggered
        assertTrue(moreClicked)
    }

    @Test
    fun homeScreen_navigationBar_reflectsSelectedState() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    uiState = HomeUiState(),
                    selectedNavItem = HomeNavigationDestination.COLLECTION
                )
            }
        }

        // Verify Collection is selected
        composeTestRule.onNodeWithText("Collection").assertIsSelected()

        // Verify other items are not selected
        composeTestRule.onNodeWithText("Home").assertIsNotSelected()
        composeTestRule.onNodeWithText("Plays").assertIsNotSelected()
        composeTestRule.onNodeWithText("Profile").assertIsNotSelected()
    }
}
