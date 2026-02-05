package app.meeplebook.feature.plays

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.testutils.stringRes
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun whenLoadingState_thenShowsLoadingIndicator() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Loading,
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsLoadingIndicator").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_loading)).assertIsDisplayed()
        composeRule.onNodeWithTag("playsScreen").assertIsDisplayed()
    }

    @Test
    fun whenEmptyStateNoPlays_thenShowsEmptyMessage() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_PLAYS,
                        common = PlaysCommonState(
                            playStats = createTestStats()
                        )
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsEmptyState").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_empty)).assertIsDisplayed()
    }

    @Test
    fun whenEmptyStateNoSearchResults_thenShowsSearchEmptyMessage() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_SEARCH_RESULTS,
                        common = PlaysCommonState(
                            searchQuery = "monopoly",
                            playStats = createTestStats()
                        )
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsEmptyState").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.plays_search_no_results)).assertIsDisplayed()
    }

    @Test
    fun whenErrorState_thenShowsErrorMessage() {
        val errorText = uiTextRes(R.string.sync_plays_failed_error)

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Error(
                        errorMessageUiText = errorText,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsErrorState").assertIsDisplayed()
        composeRule.onNodeWithText(stringRes(R.string.sync_plays_failed_error)).assertIsDisplayed()
    }

    // endregion

    // region Content State Tests

    @Test
    fun whenContentState_thenShowsStatsCard() {
        val testStats = PlayStats(
            uniqueGamesCount = 25,
            totalPlays = 150,
            playsThisYear = 42,
            currentYear = 2026
        )

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Content(
                        sections = emptyList(),
                        common = PlaysCommonState(playStats = testStats)
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
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
    fun whenContentState_thenShowsSearchInput() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Content(
                        sections = emptyList(),
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsSearchInput").assertIsDisplayed()
    }

    @Test
    fun whenContentStateWithPlays_thenShowsPlayCards() {
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
                    currentState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
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
    fun whenContentStateWithMultipleSections_thenShowsMonthHeaders() {
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
                    currentState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("monthHeader_2026-01").assertIsDisplayed()
        composeRule.onNodeWithTag("monthHeader_2025-12").assertExists()
    }

    // endregion

    // region Interaction Tests

    @Test
    fun whenSearchInputChanged_thenTriggersSearchEvent() {
        var capturedSearchQuery = ""

        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_PLAYS,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = { event ->
                        if (event is PlaysEvent.SearchChanged) {
                            capturedSearchQuery = event.query
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("playsSearchInput").performTextInput("catan")

        assertEquals("catan", capturedSearchQuery)
    }

    @Test
    fun whenPlayCardTapped_thenTriggersPlayClickedEvent() {
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
                    currentState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = { event ->
                        if (event is PlaysEvent.PlayClicked) {
                            capturedPlayId = event.playId
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
    fun whenEmptyState_thenStillShowsStatsAndSearch() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Empty(
                        reason = EmptyReason.NO_PLAYS,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsStatsCard").assertIsDisplayed()
        composeRule.onNodeWithTag("playsSearchInput").assertIsDisplayed()
    }

    @Test
    fun whenErrorState_thenStillShowsStatsAndSearch() {
        composeRule.setContent {
            MeepleBookTheme {
                PlaysScreenRoot(
                    currentState = PlaysUiState.Error(
                        errorMessageUiText = uiText("Test error"),
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playsStatsCard").assertIsDisplayed()
        composeRule.onNodeWithTag("playsSearchInput").assertIsDisplayed()
    }

    @Test
    fun whenPlayHasSyncedStatus_thenShowsGreenBadge() {
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
                    currentState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playCard_1").assertIsDisplayed()
        composeRule.onNodeWithText("Scythe").assertIsDisplayed()
    }

    @Test
    fun whenPlayHasPendingStatus_thenShowsOrangeBadge() {
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
                    currentState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
                )
            }
        }

        composeRule.onNodeWithTag("playCard_1").assertIsDisplayed()
        composeRule.onNodeWithText("Root").assertIsDisplayed()
    }

    @Test
    fun whenPlayHasLocation_thenShowsLocation() {
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
                    currentState = PlaysUiState.Content(
                        sections = testSections,
                        common = PlaysCommonState(playStats = createTestStats())
                    ),
                    snackbarState = SnackbarHostState(),
                    onUserAction = {}
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
        id = id,
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
