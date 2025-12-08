package app.meeplebook.core.network

import kotlinx.coroutines.delay
import kotlin.random.Random

class RetryException(
    message: String,
    val username: String,
    val lastHttpCode: Int?,
    val attempts: Int,
    val lastDelayMs: Long
) : Exception(message)

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
