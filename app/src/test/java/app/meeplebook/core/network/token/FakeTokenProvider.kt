package app.meeplebook.core.network.token

/**
 * Fake implementation of [TokenProvider] for testing.
 * Allows tests to control the returned token value.
 */
class FakeTokenProvider(
    private var token: String = ""
) : TokenProvider {

    override fun getBggToken(): String = token

    /**
     * Sets the token value that will be returned by [getBggToken].
     */
    fun setToken(value: String) {
        token = value
    }
}
