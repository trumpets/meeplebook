package app.meeplebook.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.HiltTestActivity
import app.meeplebook.ui.theme.MeepleBookTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Compose tests for [HomeScreenContent].
 * Tests UI rendering and interaction behavior for different home screen states.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenContentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }


    @Test
    fun homeScreen_navigationBar_displaysAllItems() {
        composeTestRule.setContent {
            MeepleBookTheme {
                HomeScreenContent(
                    tabNavController = rememberNavController()
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
                HomeScreenContent(
                    onNavItemClick = { selectedDestination = it },
                    tabNavController = rememberNavController()
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
                HomeScreenContent(
                    tabNavController = rememberNavController()
                )
            }
        }

        // Click on Collection nav item
        composeTestRule.onNodeWithText("Collection").performClick()

        // Verify Collection is selected
        composeTestRule.onNodeWithText("Collection").assertIsSelected()

        // Verify other items are not selected
        composeTestRule.onNodeWithText("Home").assertIsNotSelected()
        composeTestRule.onNodeWithText("Plays").assertIsNotSelected()
        composeTestRule.onNodeWithText("Profile").assertIsNotSelected()
    }
}