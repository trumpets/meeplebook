package app.meeplebook.testutils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.time.Duration

/**
 * Advances time past a debounce and flushes coroutines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.advanceDebounce(debounceTime: Duration) {
    advanceTimeBy(debounceTime.inWholeMilliseconds + 1)
    advanceUntilIdle()
}

/**
 * Await the next UI state after debounce.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> ReceiveTurbine<T>.awaitAfterDebounce(
    scope: TestScope,
    debounceTime: Duration
): T {
    scope.advanceDebounce(debounceTime)
    return awaitItem()
}

/**
 * Awaits the next non-skipped state from a [StateFlow] and asserts it is [T].
 *
 * - Skips the first emission (the current StateFlow value).
 * - If [debounceTime] is non-null, advances time before each awaited emission.
 */
suspend inline fun <S, reified T: S> TestScope.awaitUiState(
    uiState: StateFlow<S>,
    debounceTime: Duration? = null,
    crossinline skipWhile: (S) -> Boolean = { false }
): T {
    var content: T? = null

    uiState.test {
        // Skip initial StateFlow value
        skipItems(1)

        // Drain initial states to avoid flakiness from assuming a fixed number of emissions
        var state: S
        do {
            state = if (debounceTime == null) {
                awaitItem()
            } else {
                awaitAfterDebounce(scope = this@awaitUiState, debounceTime = debounceTime)
            }
        } while (skipWhile(state))

        content = state.assertState<T>()

        cancelAndIgnoreRemainingEvents()
    }

    return requireNotNull(content) {
        "Expected ${T::class.simpleName} after debounce but none was emitted"
    }
}

inline fun <reified T> Any?.assertState(): T =
    this as? T
        ?: error("Expected ${T::class.simpleName} but was ${this?.let { it::class.simpleName } ?: "null"}: $this")