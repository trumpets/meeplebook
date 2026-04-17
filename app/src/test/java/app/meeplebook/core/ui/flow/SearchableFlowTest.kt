package app.meeplebook.core.ui.flow

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [searchableFlow].
 *
 * Tests the searchable flow's behavior including:
 * - Immediate emission for empty queries
 * - Debouncing for non-empty queries
 * - Trimming and distinct until changed behavior
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchableFlowTest {

    @Test
    fun `empty query emits immediately without debounce`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        // When
        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        // Then
        searchFlow.test {
            // Empty query should emit immediately
            assertEquals("result:", awaitItem())
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-empty query is debounced`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - send non-empty query
            queryFlow.value = "azul"

            // Then - should not emit immediately
            expectNoEvents()

            // Advance time but not past debounce
            advanceTimeBy(debounceMs - 1)
            expectNoEvents()

            // Advance past debounce
            advanceTimeBy(2)
            assertEquals("result:azul", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query is trimmed before processing`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - send query with leading/trailing spaces
            queryFlow.value = "  azul  "

            // Then - after debounce, trimmed query is used
            advanceTimeBy(debounceMs + 1)
            assertEquals("result:azul", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whitespace-only query is treated as empty`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("start")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial query for start
            assertEquals("result:start", awaitItem())

            // When - send whitespace-only query
            queryFlow.value = "   "

            // Then - should emit immediately without debounce (treated as empty)
            assertEquals("result:", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distinctUntilChanged prevents duplicate emissions`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - send same empty query again
            queryFlow.value = ""
            
            // Then - no new emission
            expectNoEvents()

            // When - send a new query
            queryFlow.value = "wingspan"
            advanceTimeBy(debounceMs + 1)

            // Then - should emit
            assertEquals("result:wingspan", awaitItem())

            // When - send same query again
            queryFlow.value = "wingspan"
            advanceTimeBy(debounceMs + 1)

            // Then - no new emission due to distinctUntilChanged
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rapid typing only emits final query after debounce`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - simulate rapid typing
            queryFlow.value = "a"
            advanceTimeBy(50)
            queryFlow.value = "az"
            advanceTimeBy(50)
            queryFlow.value = "azu"
            advanceTimeBy(50)
            queryFlow.value = "azul"

            // Then - no emissions yet
            expectNoEvents()

            // Advance past debounce from last keystroke
            advanceTimeBy(debounceMs + 1)

            // Only final query is emitted
            assertEquals("result:azul", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching from non-empty to empty emits immediately`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - send non-empty query
            queryFlow.value = "wingspan"
            advanceTimeBy(debounceMs + 1)
            assertEquals("result:wingspan", awaitItem())

            // When - clear query
            queryFlow.value = ""

            // Then - should emit immediately without debounce
            assertEquals("result:", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `block function receives correct query parameter`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L
        val receivedQueries = mutableListOf<String>()

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query ->
                receivedQueries.add(query)
                flowOf("result:$query")
            }
        )

        searchFlow.test {
            // Drain initial emission
            awaitItem()
            assertEquals(listOf(""), receivedQueries)

            // When - send query
            queryFlow.value = "test"
            advanceTimeBy(debounceMs + 1)
            awaitItem()

            // Then - block received trimmed query
            assertEquals(listOf("", "test"), receivedQueries)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `consecutive empty queries from trimming do not emit multiple times`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - send various whitespace-only queries
            queryFlow.value = " "
            expectNoEvents() // distinctUntilChanged prevents re-emission

            queryFlow.value = "  "
            expectNoEvents() // distinctUntilChanged prevents re-emission

            queryFlow.value = "\t"
            expectNoEvents() // distinctUntilChanged prevents re-emission

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `block can return flow with multiple items`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val debounceMs = 300L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = debounceMs,
            block = { query ->
                flowOf("first:$query", "second:$query", "third:$query")
            }
        )

        searchFlow.test {
            // Initial empty query - block can emit multiple items
            assertEquals("first:", awaitItem())
            assertEquals("second:", awaitItem())
            assertEquals("third:", awaitItem())

            // When - send non-empty query
            queryFlow.value = "test"
            advanceTimeBy(debounceMs + 1)

            // Then - all items from block are emitted
            assertEquals("first:test", awaitItem())
            assertEquals("second:test", awaitItem())
            assertEquals("third:test", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `custom debounce duration is respected`() = runTest {
        // Given
        val queryFlow = MutableStateFlow("")
        val customDebounceMs = 500L

        val searchFlow = searchableFlow(
            queryFlow = queryFlow,
            debounceMillis = customDebounceMs,
            block = { query -> flowOf("result:$query") }
        )

        searchFlow.test {
            // Initial empty query
            assertEquals("result:", awaitItem())

            // When - send non-empty query
            queryFlow.value = "test"

            // Advance to just before custom debounce
            advanceTimeBy(customDebounceMs - 1)
            expectNoEvents()

            // Advance past custom debounce
            advanceTimeBy(2)
            assertEquals("result:test", awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
