package app.meeplebook.testutils

inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        throw AssertionError("Expected ${T::class}, but got ${e::class}", e)
    }
    throw AssertionError("Expected ${T::class}, but nothing was thrown")
}