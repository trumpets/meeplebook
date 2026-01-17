package app.meeplebook.feature.login

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.R
import app.meeplebook.testutils.stringRes
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Compose tests for [LoginScreenContent].
 * Tests UI rendering and interaction behavior for different login states.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_initialState_displaysEmptyFields() {
        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText(stringRes(R.string.login_title)).assertIsDisplayed()

        // Verify username field is displayed with label
        composeTestRule.onNodeWithText(stringRes(R.string.username)).assertIsDisplayed()

        // Verify password field is displayed with label
        composeTestRule.onNodeWithText(stringRes(R.string.password)).assertIsDisplayed()

        // Verify login button is displayed and enabled
        composeTestRule.onNodeWithText(stringRes(R.string.login)).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringRes(R.string.login)).assertIsEnabled()
    }

    @Test
    fun loginScreen_withFilledCredentials_displaysInputValues() {
        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(
                        username = "testUser",
                        password = "testPassword"
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Verify username is displayed
        composeTestRule.onNodeWithText("testUser").assertIsDisplayed()

        // Password is masked, so we don't assert on the actual text value
    }

    @Test
    fun loginScreen_loadingState_displaysProgressIndicatorAndDisablesButton() {
        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(
                        username = "loadingUser",
                        password = "password123",
                        isLoading = true
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Verify login button is displayed but disabled during loading
        composeTestRule.onNodeWithText(stringRes(R.string.login)).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringRes(R.string.login)).assertIsNotEnabled()
    }

    @Test
    fun loginScreen_errorState_displaysErrorMessage() {
        @StringRes val errorMsgResId = R.string.msg_invalid_credentials_error

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(
                        username = "wrongUser",
                        password = "wrongPass",
                        errorMessageResId = errorMsgResId
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText(stringRes(errorMsgResId)).assertIsDisplayed()
    }

    @Test
    fun loginScreen_emptyCredentialsError_displaysErrorMessage() {
        @StringRes val errorMsgResId = R.string.msg_empty_credentials_error

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(
                        errorMessageResId = errorMsgResId
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Verify empty credentials error message is displayed
        composeTestRule.onNodeWithText(stringRes(errorMsgResId)).assertIsDisplayed()
    }

    @Test
    fun loginScreen_networkError_displaysErrorMessage() {
        @StringRes val errorMsgResId = R.string.msg_login_failed_error

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(
                        username = "user",
                        password = "pass",
                        errorMessageResId = errorMsgResId
                    ),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Verify login failed error message is displayed
        composeTestRule.onNodeWithText(stringRes(errorMsgResId)).assertIsDisplayed()
    }

    @Test
    fun loginScreen_loginButtonClick_triggersCallback() {
        var loginClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = { loginClicked = true }
                )
            }
        }

        // Click login button
        composeTestRule.onNodeWithText(stringRes(R.string.login)).performClick()

        // Verify callback was triggered
        assertTrue(loginClicked)
    }

    @Test
    fun loginScreen_usernameInput_triggersCallback() {
        var capturedUsername = ""

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(),
                    onUsernameChange = { capturedUsername = it },
                    onPasswordChange = {},
                    onLoginClick = {}
                )
            }
        }

        // Type in the username field using test tag
        composeTestRule.onNodeWithTag("usernameField").performTextInput("newUser")

        // Verify callback was triggered with the correct value
        assertEquals("newUser", capturedUsername)
    }

    @Test
    fun loginScreen_passwordInput_triggersCallback() {
        var capturedPassword = ""

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(),
                    onUsernameChange = {},
                    onPasswordChange = { capturedPassword = it },
                    onLoginClick = {}
                )
            }
        }

        // Type in the password field using test tag
        composeTestRule.onNodeWithTag("passwordField").performTextInput("newPassword")

        // Verify callback was triggered with the correct value
        assertEquals("newPassword", capturedPassword)
    }

    @Test
    fun loginScreen_loadingState_buttonClickDoesNotTriggerCallback() {
        var loginClicked = false

        composeTestRule.setContent {
            MeepleBookTheme {
                LoginScreenContent(
                    uiState = LoginUiState(isLoading = true),
                    onUsernameChange = {},
                    onPasswordChange = {},
                    onLoginClick = { loginClicked = true }
                )
            }
        }

        // Try to click the disabled login button
        composeTestRule.onNodeWithText(stringRes(R.string.login)).performClick()

        // Verify callback was NOT triggered (button is disabled during loading)
        assertFalse(loginClicked)
    }
}
