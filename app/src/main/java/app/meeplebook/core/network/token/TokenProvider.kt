package app.meeplebook.core.network.token

/**
 * Interface for providing the BGG bearer token.
 * This abstraction allows for easy testing with fake implementations.
 */
interface TokenProvider {
    /**
     * Retrieves the BGG bearer token.
     * Returns empty string if token is not configured.
     */
    fun getBggToken(): String
}
