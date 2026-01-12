package app.meeplebook.testutils

import app.cash.turbine.ReceiveTurbine
import kotlinx.coroutines.ExperimentalCoroutinesApi
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