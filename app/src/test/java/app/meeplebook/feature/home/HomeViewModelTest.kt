package app.meeplebook.feature.home

import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.GetCollectionUseCase
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlaysResponse
import app.meeplebook.core.plays.domain.GetPlaysUseCase
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayGame
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HomeViewModelTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var getCollectionUseCase: GetCollectionUseCase
    private lateinit var getPlaysUseCase: GetPlaysUseCase
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testCredentials = AuthCredentials("testUser", "password")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        getCollectionUseCase = GetCollectionUseCase(fakeCollectionRepository)
        getPlaysUseCase = GetPlaysUseCase(fakePlaysRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            authRepository = fakeAuthRepository,
            getCollectionUseCase = getCollectionUseCase,
            getPlaysUseCase = getPlaysUseCase
        )
    }

    private fun createTestCollectionItem(id: Long = 1L) = CollectionItem(
        objectId = id,
        name = "Catan",
        yearPublished = 1995,
        thumbnailUrl = null,
        imageUrl = null,
        numPlays = 5,
        owned = true,
        rating = null,
        averageRating = null
    )

    private fun createTestPlay(id: Long = 1L) = Play(
        playId = id,
        date = "2024-01-15",
        quantity = 1,
        length = 60,
        incomplete = false,
        noWinStats = false,
        location = "Home",
        comments = null,
        game = PlayGame(objectId = 100L, name = "Catan"),
        players = emptyList()
    )

    @Test
    fun `initial state shows not logged in error when no user`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.msg_not_logged_in, state.errorMessageResId)
        assertFalse(state.isLoading)
    }

    @Test
    fun `initial state loads data when user is logged in`() = runTest {
        // Set up logged in user
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        val collectionItems = listOf(createTestCollectionItem())
        fakeCollectionRepository.getCollectionResult = AppResult.Success(collectionItems)

        val plays = listOf(createTestPlay())
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(plays, totalPlays = 50, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("testUser", state.username)
        assertEquals(collectionItems, state.collection)
        assertEquals(plays, state.recentPlays)
        assertEquals(50, state.totalPlays)
        assertFalse(state.isLoading)
        assertNull(state.errorMessageResId)
    }

    @Test
    fun `collection network error shows error message`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Failure(CollectionError.NetworkError)
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(emptyList(), totalPlays = 0, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.msg_network_error, state.errorMessageResId)
    }

    @Test
    fun `collection timeout shows timeout error`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Failure(CollectionError.Timeout)
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(emptyList(), totalPlays = 0, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.msg_collection_timeout, state.errorMessageResId)
    }

    @Test
    fun `plays network error shows error message`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.getPlaysResult = AppResult.Failure(PlaysError.NetworkError)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.msg_network_error, state.errorMessageResId)
    }

    @Test
    fun `refresh reloads data`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(emptyList(), totalPlays = 0, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // Update data
        val newItems = listOf(createTestCollectionItem())
        fakeCollectionRepository.getCollectionResult = AppResult.Success(newItems)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(newItems, state.collection)
    }

    @Test
    fun `loadMorePlays loads next page`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(emptyList())

        val firstPagePlays = listOf(createTestPlay(1L))
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(firstPagePlays, totalPlays = 150, currentPage = 1, hasMorePages = true)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasMorePlays)

        // Load second page
        val secondPagePlays = listOf(createTestPlay(2L))
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(secondPagePlays, totalPlays = 150, currentPage = 2, hasMorePages = false)
        )

        viewModel.loadMorePlays()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.recentPlays.size)
        assertEquals(2, state.playsCurrentPage)
        assertFalse(state.hasMorePlays)
    }

    @Test
    fun `loadMorePlays does nothing when no more pages`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(emptyList(), totalPlays = 50, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val initialCallCount = fakePlaysRepository.getPlaysCallCount

        viewModel.loadMorePlays()
        advanceUntilIdle()

        assertEquals(initialCallCount, fakePlaysRepository.getPlaysCallCount)
    }

    @Test
    fun `hasData returns true when collection is not empty`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(listOf(createTestCollectionItem()))
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(emptyList(), totalPlays = 0, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasData)
    }

    @Test
    fun `hasData returns true when plays is not empty`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(listOf(createTestPlay()), totalPlays = 1, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasData)
    }

    @Test
    fun `hasData returns false when both collection and plays are empty`() = runTest {
        fakeAuthRepository.loginResult = AppResult.Success(testCredentials)
        fakeAuthRepository.login(testCredentials.username, testCredentials.password)

        fakeCollectionRepository.getCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.getPlaysResult = AppResult.Success(
            PlaysResponse(emptyList(), totalPlays = 0, currentPage = 1, hasMorePages = false)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasData)
    }
}
