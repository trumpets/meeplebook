package app.meeplebook.core.network.token

/**
 * Fake implementation of [TokenProviding] for testing.
 * Allows tests to control the returned token value.
 */
class FakeTokenProvider(
    private var token: String = ""
) : TokenProviding {

    override fun getBggToken(): String = token

    /**
     * Sets the token value that will be returned by [getBggToken].
     */
    fun setToken(value: String) {
        token = value
    }
}
