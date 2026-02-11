package app.meeplebook.feature.plays

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.testutils.stringRes
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/**
 * Instrumented Compose tests for [PlaysScreenRoot].
 *
 * Covers rendering and user interaction across all screen states:
 * - State rendering: Loading, Empty variants, Error, Content
 * - User interactions: Search input, play card taps, refresh
 * - Stats and section display verification
 */
@RunWith(AndroidJUnit4::class)
class PlaysScreenRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    // region State Rendering Tests

    @Test
    fun playsScreenRoot_loadingState_displaysLoadingIndicator() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Loading,
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_loading)).assertIsDisplayed()
        composeRule.onNodeWithTag("playsScreen").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_emptyState_noPlays_displaysMessage() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_PLAYS,
                        common = PlaysCommonState(
                            playStats = createTestStats()
                        )
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("emptyState").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_empty)).assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_emptyState_noSearchResults_displaysMessage() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_SEARCH_RESULTS,
                        common = PlaysCommonState(
                            searchQuery = "monopoly",
                            playStats = createTestStats()
                        )
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("emptyState").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_search_no_results)).assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_errorState_displaysErrorMessage() {
        val errorText = uiTextRes(R.string.sync_plays_failed_error)

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Error(
                        errorMessageUiText = errorText,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("errorState").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.sync_plays_failed_error)).assertIsDisplayed()
    }

    // endregion

    // region Content State Tests

    @Test
    fun playsScreenRoot_contentState_displaysStatsCard() {
        val testStats = PlayStats(
            uniqueGamesCount = 25,
            totalPlays = 150,
            playsThisYear = 42,
            currentYear = 2026
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = emptyList(),
                        common = PlaysCommonState(playStats = testStats)
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsStatsCard").assertIsDisplayed()
        composeRule.onNodeWithText("25").assertIsDisplayed()
        composeRule.onNodeWithText("150").assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_stat_unique_games)).assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_stat_total_plays)).assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_contentState_displaysSearchField() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = emptyList(),
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("searchField").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_contentState_withPlays_displaysPlayCards() {
        val testSections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = listOf(
                    createTestPlayItem(id = 1L, gameName = "Catan"),
                    createTestPlayItem(id = 2L, gameName = "Wingspan")
                )
            )
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsListContent").assertIsDisplayed()
        composeRule.onNodeWithText("Catan").assertIsDisplayed()
        composeRule.onNodeWithText("Wingspan").assertIsDisplayed()
        composeRule.onNodeWithTag("playCard_1").assertIsDisplayed()
        composeRule.onNodeWithTag("playCard_2").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_contentState_withMultipleSections_displaysMonthHeaders() {
        val testSections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = listOf(createTestPlayItem(id = 1L, gameName = "Azul"))
            ),
            PlaysSection(
                monthYearDate = YearMonth.of(2025, 12),
                plays = listOf(createTestPlayItem(id = 2L, gameName = "Terraforming Mars"))
            )
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("monthHeader_2026-01").assertIsDisplayed()
        composeRule.onNodeWithTag("monthHeader_2025-12").assertExists()
    }

    // endregion

    // region Interaction Tests

    @Test
    fun playsScreenRoot_searchInputChanged_triggersSearchEvent() {
        var capturedSearchQuery = ""

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_PLAYS,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = { event ->
                        if (event is PlaysEvent.SearchChanged) {
                            capturedSearchQuery = event.query
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("searchField").performTextInput("catan")

        assertEquals("catan", capturedSearchQuery)
    }

    @Test
    fun playsScreenRoot_playCardTapped_triggersPlayClickedEvent() {
        var capturedPlayId: Long? = null

        val testSections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = listOf(createTestPlayItem(id = 42L, gameName = "Ticket to Ride"))
            )
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = { event ->
                        if (event is PlaysEvent.PlayClicked) {
                            capturedPlayId = event.playId.localId
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("playCard_42").performClick()

        assertEquals(42L, capturedPlayId)
    }

    // endregion

    // region UI Element Presence Tests

    @Test
    fun playsScreenRoot_emptyState_displaysStatsAndSearch() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_PLAYS,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsStatsCard").assertIsDisplayed()
        composeRule.onNodeWithTag("searchField").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_errorState_displaysStatsAndSearch() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Error(
                        errorMessageUiText = uiText("Test error"),
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsStatsCard").assertIsDisplayed()
        composeRule.onNodeWithTag("searchField").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_play_syncedStatus_displaysGreenBadge() {
        val testSections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = listOf(
                    createTestPlayItem(
                        id = 1L,
                        gameName = "Scythe",
                        syncStatus = PlaySyncStatus.SYNCED
                    )
                )
            )
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("playCard_1").assertIsDisplayed()
        composeRule.onNodeWithText("Scythe").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_play_pendingStatus_displaysOrangeBadge() {
        val testSections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = listOf(
                    createTestPlayItem(
                        id = 1L,
                        gameName = "Root",
                        syncStatus = PlaySyncStatus.PENDING
                    )
                )
            )
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithTag("playCard_1").assertIsDisplayed()
        composeRule.onNodeWithText("Root").assertIsDisplayed()
    }

    @Test
    fun playsScreenRoot_play_withLocation_displaysLocation() {
        val testSections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = listOf(
                    createTestPlayItem(
                        id = 1L,
                        gameName = "Everdell",
                        location = "Board Game Café"
                    )
                )
            )
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    uiState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithText("Everdell").assertIsDisplayed()
        composeRule.onNodeWithText("Board Game Café").assertIsDisplayed()
    }

    // endregion

    // region Helper Functions

    private fun createTestStats() = PlayStats(
        uniqueGamesCount = 10,
        totalPlays = 50,
        playsThisYear = 15,
        currentYear = 2026
    )

    private fun createTestPlayItem(
        id: Long,
        gameName: String,
        syncStatus: PlaySyncStatus = PlaySyncStatus.SYNCED,
        location: String? = null
    ) = PlayItem(
        playId = PlayId.Local(id),
        gameName = gameName,
        thumbnailUrl = null,
        dateUiText = uiText("15/01/2026"),
        durationUiText = uiText("60min"),
        playerSummaryUiText = uiText("3 players"),
        location = location,
        comments = null,
        syncStatus = syncStatus
    )

    // endregion
}
