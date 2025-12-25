package app.meeplebook.feature.collection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
