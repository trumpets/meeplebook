package app.meeplebook.feature.addplay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.testutils.stringRes
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Instrumented Compose tests for [AddPlayScreenRoot].
 *
 * Uses pre-constructed [AddPlayUiState] instances — no ViewModel or Hilt wiring.
 * Tests are organised into groups mirroring the two main state branches:
 *
 * - [AddPlayUiState.GameSearch]: rendering and interaction before a game is selected
 * - [AddPlayUiState.GameSelected]: rendering, optional fields, location, players,
 *   save/cancel actions, and the discard-play dialog
 */
@RunWith(AndroidJUnit4::class)
class AddPlayScreenRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── Group 1: GameSearch — rendering ──────────────────────────────────────

    @Test
    fun gameSearch_emptyQuery_showsSearchFieldAndAllCollection() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSearchState(), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("gameSearchField").assertIsDisplayed()
        composeRule.onNodeWithTag("gameSearchResults").assertIsDisplayed()
    }

    @Test
    fun gameSearch_withResults_showsResultsList() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSearchState(query = "Cat", hasResults = true), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("gameSearchResults").assertIsDisplayed()
        composeRule.onNodeWithTag("gameSearchResult_1").assertIsDisplayed()
        composeRule.onNodeWithTag("gameSearchResult_2").assertIsDisplayed()
    }

    @Test
    fun gameSearch_topBarTitle_showsLogPlayTitle() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSearchState(), onEvent = {})
            }
        }

        composeRule.onNodeWithText(stringRes(R.string.add_play_title)).assertIsDisplayed()
    }

    // ── Group 2: GameSearch — interactions ───────────────────────────────────

    @Test
    fun gameSearch_typeQuery_emitsSearchQueryChangedEvent() {
        var captured: AddPlayEvent? = null

        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSearchState(), onEvent = { captured = it })
            }
        }

        composeRule.onNodeWithTag("gameSearchField").performTextInput("Catan")

        assertTrue(captured is AddPlayEvent.GameSearchEvent.GameSearchQueryChanged)
        assertEquals(
            "Catan",
            (captured as AddPlayEvent.GameSearchEvent.GameSearchQueryChanged).query,
        )
    }

    @Test
    fun gameSearch_tapResult_emitsGameSelectedEvent() {
        var captured: AddPlayEvent? = null

        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSearchState(query = "Catan", hasResults = true),
                    onEvent = { captured = it },
                )
            }
        }

        composeRule.onNodeWithTag("gameSearchResult_1").performClick()

        assertTrue(captured is AddPlayEvent.GameSearchEvent.GameSelected)
        assertEquals(1L, (captured as AddPlayEvent.GameSearchEvent.GameSelected).gameId)
    }

    // ── Group 3: GameSelected — rendering ────────────────────────────────────

    @Test
    fun gameSelected_topBarTitle_showsGameName() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(), onEvent = {})
            }
        }

        composeRule.onNodeWithText("Catan").assertIsDisplayed()
    }

    @Test
    fun gameSelected_defaultState_dateAndDurationFieldsVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("dateField").assertIsDisplayed()
        composeRule.onNodeWithTag("durationField").assertIsDisplayed()
    }

    @Test
    fun gameSelected_defaultState_locationFieldVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("locationField").assertIsDisplayed()
    }

    @Test
    fun gameSelected_defaultState_optionalFieldsHidden() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(
                        showQuantity = false,
                        showIncomplete = false,
                        showComments = false,
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag("quantityField").assertDoesNotExist()
        composeRule.onNodeWithTag("incompleteToggle").assertDoesNotExist()
        composeRule.onNodeWithTag("commentsField").assertDoesNotExist()
    }

    // ── Group 4: GameSelected — optional fields ───────────────────────────────

    @Test
    fun gameSelected_showQuantityTrue_quantityFieldVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(showQuantity = true), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("quantityField").assertIsDisplayed()
    }

    @Test
    fun gameSelected_showIncompleteTrue_incompleteToggleVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(showIncomplete = true), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("incompleteToggle").assertIsDisplayed()
    }

    @Test
    fun gameSelected_showCommentsTrue_commentsFieldVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(showComments = true), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("commentsField").assertIsDisplayed()
    }

    // ── Group 5: GameSelected — location section ──────────────────────────────

    @Test
    fun gameSelected_recentLocations_chipRowVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(
                        location = LocationState(
                            value = "Home",
                            suggestions = emptyList(),
                            recentLocations = listOf("Home", "Game Café")
                        ),
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag("recentLocationChips").assertIsDisplayed()
    }

    @Test
    fun gameSelected_locationSuggestions_suggestionsListVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(
                        location = LocationState(
                            value = "Gam",
                            suggestions = listOf("Game Café", "Game Vault"),
                            recentLocations = emptyList()
                        ),
                    ),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag("locationField").performClick()

        composeRule.onNodeWithText("Game Café").assertIsDisplayed()
        composeRule.onNodeWithText("Game Vault").assertIsDisplayed()
        composeRule.onNodeWithTag("locationSuggestions").assertIsDisplayed()
    }

    // ── Group 6: GameSelected — players section ───────────────────────────────

    @Test
    fun gameSelected_withPlayers_playerRowsVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(players = testPlayers()),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag("playerEntry_Alice").assertIsDisplayed()
        composeRule.onNodeWithTag("playerEntry_Bob").assertIsDisplayed()
    }

    @Test
    fun gameSelected_playerSuggestions_chipRowVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(suggestions = testSuggestions(count = 3)),
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag("playerSuggestionChips").assertIsDisplayed()
    }

    @Test
    fun gameSelected_moreThanTenSuggestions_moreChipVisible() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(suggestions = testSuggestions(count = 11)),
                    onEvent = {},
                )
            }
        }

        composeRule
            .onNodeWithTag("playerSuggestionChips")
            .performScrollToNode(hasTestTag("morePlayersChip"))

        composeRule.onNodeWithTag("morePlayersChip").assertIsDisplayed()
    }

    // ── Group 7: Save / Cancel ────────────────────────────────────────────────

    @Test
    fun gameSelected_saveButton_isEnabledAndEmitsSaveClicked() {
        var captured: AddPlayEvent? = null

        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(
                    uiState = gameSelectedState(isSaving = false),
                    onEvent = { captured = it },
                )
            }
        }

        composeRule.onNodeWithText(stringRes(R.string.add_play_save))
            .assertIsEnabled()
            .performClick()

        assertTrue(captured is AddPlayEvent.ActionEvent.SaveClicked)
    }

    @Test
    fun gameSelected_isSaving_showsSpinnerAndHidesSaveButton() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(isSaving = true), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("saveButton").assertDoesNotExist()
    }

    @Test
    fun gameSearch_backButton_emitsCancelClicked() {
        var captured: AddPlayEvent? = null

        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSearchState(), onEvent = { captured = it })
            }
        }

        composeRule
            .onNodeWithContentDescription(stringRes(R.string.add_play_cancel))
            .performClick()

        assertTrue(captured is AddPlayEvent.ActionEvent.CancelClicked)
    }

    // ── Group 8: Discard dialog ───────────────────────────────────────────────

    @Test
    fun gameSelected_backPressed_showsDiscardDialog() {
        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(), onEvent = {})
            }
        }

        Espresso.pressBack()

        composeRule.onNodeWithText(stringRes(R.string.add_play_discard_title)).assertIsDisplayed()
    }

    @Test
    fun gameSelected_discardConfirmed_emitsCancelClicked() {
        var captured: AddPlayEvent? = null

        composeRule.setContent {
            MeepleBookTheme {
                AddPlayScreenRoot(uiState = gameSelectedState(), onEvent = { captured = it })
            }
        }

        Espresso.pressBack()
        composeRule.onNodeWithText(stringRes(R.string.add_play_discard_confirm)).performClick()

        assertTrue(captured is AddPlayEvent.ActionEvent.CancelClicked)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun gameSearchState(
        query: String = "",
        hasResults: Boolean = false,
    ) = AddPlayUiState.GameSearch(
        gameSearchQuery = query,
        gameSearchResults = if (hasResults) listOf(
            SearchResultGameItem(1L, "Catan", 1995, null),
            SearchResultGameItem(2L, "Ticket to Ride", 2004, null),
        ) else emptyList(),
    )

    private fun gameSelectedState(
        players: List<PlayerEntryUi> = emptyList(),
        suggestions: List<PlayerSuggestion> = emptyList(),
        location: LocationState = LocationState(
            value = null,
            suggestions = emptyList(),
            recentLocations = emptyList()
        ),
        showQuantity: Boolean = false,
        showIncomplete: Boolean = false,
        showComments: Boolean = false,
        isSaving: Boolean = false,
    ) = AddPlayUiState.GameSelected(
        gameId = 13L,
        gameName = "Catan",
        date = Instant.parse("2026-03-30T18:00:00Z"),
        durationMinutes = null,
        location = location,
        players = PlayersState(players = players, colorsHistory = emptyList()),
        playersByLocation = suggestions,
        showQuantity = showQuantity,
        showIncomplete = showIncomplete,
        showComments = showComments,
        isSaving = isSaving,
    )

    private fun testPlayers() = listOf(
        PlayerEntryUi(
            playerIdentity = PlayerIdentity("Alice", username = null, userId = null),
            startPosition = 1,
            color = PlayerColor.BLUE.colorString,
            score = null,
            isWinner = false,
        ),
        PlayerEntryUi(
            playerIdentity = PlayerIdentity("Bob", username = null, userId = null),
            startPosition = 2,
            color = null,
            score = null,
            isWinner = false,
        ),
    )

    private fun testSuggestions(count: Int) = (1..count).map { i ->
        PlayerSuggestion(PlayerIdentity("Player$i", username = null, userId = null))
    }
}
