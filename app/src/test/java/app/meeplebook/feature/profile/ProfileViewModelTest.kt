package app.meeplebook.feature.profile

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.model.CollectionViewMode
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.preferences.FakeUserPreferencesRepository
import app.meeplebook.core.preferences.StartingScreen
import app.meeplebook.core.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakePrefsRepository: FakeUserPreferencesRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        fakePrefsRepository = FakeUserPreferencesRepository()
        viewModel = ProfileViewModel(
            authRepository = fakeAuthRepository,
            preferencesRepository = fakePrefsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default preferences`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(StartingScreen.OVERVIEW, state.startingScreen)
        assertEquals(CollectionViewMode.LIST, state.collectionViewMode)
        assertTrue(state.collectionAlphabetJumpVisible)
        assertFalse(state.isLogoutConfirmVisible)
    }

    @Test
    fun `initial state reflects logged-in user`() = runTest {
        fakeAuthRepository.setCurrentUser(AuthCredentials("board_gamer", "pw"))
        advanceUntilIdle()
        assertEquals("board_gamer", viewModel.uiState.value.username)
    }

    @Test
    fun `state reflects preferences from repository`() = runTest {
        fakePrefsRepository.setPreferences(
            UserPreferences(
                startingScreen = StartingScreen.COLLECTION,
                collectionViewMode = CollectionViewMode.GRID,
                collectionAlphabetJumpVisible = false
            )
        )
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(StartingScreen.COLLECTION, state.startingScreen)
        assertEquals(CollectionViewMode.GRID, state.collectionViewMode)
        assertFalse(state.collectionAlphabetJumpVisible)
    }

    @Test
    fun `StartingScreenSelected persists to repository`() = runTest {
        viewModel.onEvent(ProfileEvent.StartingScreenSelected(StartingScreen.COLLECTION))
        advanceUntilIdle()
        assertEquals(StartingScreen.COLLECTION, fakePrefsRepository.preferences.value.startingScreen)
    }

    @Test
    fun `CollectionViewModeSelected persists to repository`() = runTest {
        viewModel.onEvent(ProfileEvent.CollectionViewModeSelected(CollectionViewMode.GRID))
        advanceUntilIdle()
        assertEquals(CollectionViewMode.GRID, fakePrefsRepository.preferences.value.collectionViewMode)
    }

    @Test
    fun `CollectionAlphabetJumpVisibilityChanged persists to repository`() = runTest {
        viewModel.onEvent(ProfileEvent.CollectionAlphabetJumpVisibilityChanged(false))
        advanceUntilIdle()
        assertFalse(fakePrefsRepository.preferences.value.collectionAlphabetJumpVisible)
    }

    @Test
    fun `LogoutClicked shows confirmation dialog`() = runTest {
        viewModel.onEvent(ProfileEvent.LogoutClicked)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLogoutConfirmVisible)
    }

    @Test
    fun `LogoutDismissed hides confirmation dialog`() = runTest {
        viewModel.onEvent(ProfileEvent.LogoutClicked)
        viewModel.onEvent(ProfileEvent.LogoutDismissed)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLogoutConfirmVisible)
    }

    @Test
    fun `LogoutConfirmed calls auth logout and emits NavigateToLogin effect`() = runTest {
        fakeAuthRepository.setCurrentUser(AuthCredentials("user", "pw"))

        var receivedEffect: ProfileUiEffect? = null
        val job = launch {
            viewModel.uiEffect.collect { receivedEffect = it }
        }

        viewModel.onEvent(ProfileEvent.LogoutConfirmed)
        advanceUntilIdle()

        assertNull(fakeAuthRepository.getCurrentUser())
        assertEquals(ProfileUiEffect.NavigateToLogin, receivedEffect)

        job.cancel()
    }

    @Test
    fun `OpenSourceLicensesClicked emits OpenSourceLicenses effect`() = runTest {
        var receivedEffect: ProfileUiEffect? = null
        val job = launch {
            viewModel.uiEffect.collect { receivedEffect = it }
        }

        viewModel.onEvent(ProfileEvent.OpenSourceLicensesClicked)
        advanceUntilIdle()

        assertEquals(ProfileUiEffect.OpenSourceLicenses, receivedEffect)
        job.cancel()
    }
}
