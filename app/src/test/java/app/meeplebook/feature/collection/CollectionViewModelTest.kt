package app.meeplebook.feature.collection

import app.meeplebook.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private lateinit var viewModel: CollectionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CollectionViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        val state = viewModel.uiState.value
        assertEquals(CollectionUiState.Loading, state)
    }

    @Test
    fun `SearchChanged event does not throw ClassCastException when state is Loading`() = runTest {
        // Given: state is Loading (initial state)
        assertEquals(CollectionUiState.Loading, viewModel.uiState.value)

        // When: SearchChanged event is triggered
        // Then: should not throw ClassCastException
        viewModel.onEvent(CollectionEvent.SearchChanged("test query"))

        // State should remain Loading
        assertEquals(CollectionUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `SortSelected event does not throw ClassCastException when state is Loading`() = runTest {
        // Given: state is Loading (initial state)
        assertEquals(CollectionUiState.Loading, viewModel.uiState.value)

        // When: SortSelected event is triggered
        // Then: should not throw ClassCastException
        viewModel.onEvent(CollectionEvent.SortSelected(CollectionSort.YEAR_PUBLISHED_NEWEST))

        // State should remain Loading
        assertEquals(CollectionUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `SearchChanged event does not throw ClassCastException when state is Empty`() = runTest {
        // Given: state is Empty
        setViewModelState(CollectionUiState.Empty(EmptyReason.NO_GAMES))
        val emptyState = viewModel.uiState.value as CollectionUiState.Empty
        assertEquals(EmptyReason.NO_GAMES, emptyState.reason)

        // When: SearchChanged event is triggered
        // Then: should not throw ClassCastException
        viewModel.onEvent(CollectionEvent.SearchChanged("test query"))

        // State should remain Empty
        val resultState = viewModel.uiState.value as CollectionUiState.Empty
        assertEquals(EmptyReason.NO_GAMES, resultState.reason)
    }

    @Test
    fun `SortSelected event does not throw ClassCastException when state is Empty`() = runTest {
        // Given: state is Empty
        setViewModelState(CollectionUiState.Empty(EmptyReason.NO_SEARCH_RESULTS))
        val emptyState = viewModel.uiState.value as CollectionUiState.Empty
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, emptyState.reason)

        // When: SortSelected event is triggered
        // Then: should not throw ClassCastException
        viewModel.onEvent(CollectionEvent.SortSelected(CollectionSort.MOST_PLAYED))

        // State should remain Empty
        val resultState = viewModel.uiState.value as CollectionUiState.Empty
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, resultState.reason)
    }

    @Test
    fun `SearchChanged event does not throw ClassCastException when state is Error`() = runTest {
        // Given: state is Error
        setViewModelState(CollectionUiState.Error(R.string.collection_empty))
        val errorState = viewModel.uiState.value as CollectionUiState.Error
        assertEquals(R.string.collection_empty, errorState.errorMessageResId)

        // When: SearchChanged event is triggered
        // Then: should not throw ClassCastException
        viewModel.onEvent(CollectionEvent.SearchChanged("test query"))

        // State should remain Error
        val resultState = viewModel.uiState.value as CollectionUiState.Error
        assertEquals(R.string.collection_empty, resultState.errorMessageResId)
    }

    @Test
    fun `SortSelected event does not throw ClassCastException when state is Error`() = runTest {
        // Given: state is Error
        setViewModelState(CollectionUiState.Error(R.string.collection_search_no_results))
        val errorState = viewModel.uiState.value as CollectionUiState.Error
        assertEquals(R.string.collection_search_no_results, errorState.errorMessageResId)

        // When: SortSelected event is triggered
        // Then: should not throw ClassCastException
        viewModel.onEvent(CollectionEvent.SortSelected(CollectionSort.LEAST_PLAYED))

        // State should remain Error
        val resultState = viewModel.uiState.value as CollectionUiState.Error
        assertEquals(R.string.collection_search_no_results, resultState.errorMessageResId)
    }

    /**
     * Helper method to set the ViewModel's internal state for testing.
     * Uses reflection to access the private _uiState field.
     */
    private fun setViewModelState(state: CollectionUiState) {
        val field = viewModel.javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<CollectionUiState>
        stateFlow.value = state
    }
}
