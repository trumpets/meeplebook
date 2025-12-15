package app.meeplebook.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Compose tests for [HomeNavigationBar].
 * Tests UI rendering and interaction behavior for the home screen navigation bar.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun homeScreen_navigationBar_displaysAllItems() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeNavigationBar(
                    currentDestination = HomeNavigationDestination.HOME
                )
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
                HomeNavigationBar(
                    currentDestination = HomeNavigationDestination.HOME,
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
    fun homeScreen_navigationBar_reflectsSelectedState() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeNavigationBar(
                    currentDestination = HomeNavigationDestination.COLLECTION
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