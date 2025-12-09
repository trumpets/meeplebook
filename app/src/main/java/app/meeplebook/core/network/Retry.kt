package app.meeplebook.core.network

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Exception thrown when retry attempts are exhausted or an unrecoverable error occurs.
 *
 * @property username The username associated with the failed request.
 * @property lastHttpCode The HTTP status code of the last failed attempt, or null if not available.
 * @property attempts The total number of attempts made before failure.
 * @property lastDelayMs The delay in milliseconds before the last attempt.
 */
class RetryException(
    message: String,
    val username: String,
    val lastHttpCode: Int?,
    val attempts: Int,
    val lastDelayMs: Long
) : Exception(message)

/**
 * Executes a suspending block with exponential backoff retry logic.
 *
 * When the block throws a [RetrySignal], the function waits for an exponentially
 * increasing delay (with jitter) before retrying. If all attempts are exhausted,
 * a [RetryException] is thrown.
 *
 * @param T The return type of the block.
 * @param username The username associated with the request (used in exception details).
 * @param maxAttempts Maximum number of retry attempts (default: [MAX_RETRY_ATTEMPTS]).
 * @param initialDelayMs Initial delay in milliseconds before first retry (default: [INITIAL_RETRY_DELAY_MS]).
 * @param maxDelayMs Maximum delay in milliseconds between retries (default: [MAX_RETRY_DELAY_MS]).
 * @param backoffMultiplier Multiplier for exponential backoff (default: [BACKOFF_MULTIPLIER]).
 * @param block The suspending block to execute with retry logic.
 * @return The result of the block if successful.
 * @throws RetryException If all retry attempts are exhausted.
 */
suspend fun <T> retryWithBackoff(
    username: String,
    maxAttempts: Int = MAX_RETRY_ATTEMPTS,
    initialDelayMs: Long = INITIAL_RETRY_DELAY_MS,
    maxDelayMs: Long = MAX_RETRY_DELAY_MS,
    backoffMultiplier: Double = BACKOFF_MULTIPLIER,
    block: suspend (attempt: Int) -> T
): T {
    var delayMs = initialDelayMs
    var lastHttp: Int? = null

    repeat(maxAttempts) { attempt ->
        try {
            return block(attempt + 1)
        } catch (e: RetrySignal) {
            lastHttp = e.httpCode

            if (attempt == maxAttempts - 1) {
                throw RetryException(
                    message = "Retry attempts exceeded",
                    username = username,
                    lastHttpCode = lastHttp,
                    attempts = maxAttempts,
                    lastDelayMs = delayMs
                )
            }

            delay(delayMs)

            // exponential + jitter
            delayMs = (delayMs * backoffMultiplier + Random.nextLong(0, 500))
                .toLong()
                .coerceAtMost(maxDelayMs)
        }
    }

    error("Unexpected fallthrough")
}

/** Initial delay between retry attempts in milliseconds */
const val INITIAL_RETRY_DELAY_MS = 1000L

/** Maximum delay between retry attempts in milliseconds */
const val MAX_RETRY_DELAY_MS = 15000L

/** Backoff multiplier for retry delays */
const val BACKOFF_MULTIPLIER = 1.4

/** Maximum number of retry attempts */
const val MAX_RETRY_ATTEMPTS = 10

/**
 * Internal signal exception used to trigger retries in the backoff mechanism.
 *
 * This exception is thrown within retryable blocks to indicate that the operation
 * should be retried according to the configured backoff strategy. It is not intended
 * for general error handling.
 *
 * @property httpCode Optional HTTP status code associated with the failure.
 */
class RetrySignal(val httpCode: Int?) : Exception()
