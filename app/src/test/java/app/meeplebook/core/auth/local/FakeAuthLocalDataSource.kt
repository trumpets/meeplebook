package app.meeplebook.core.auth.local

import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [AuthLocalDataSource] for testing purposes.
 */
class FakeAuthLocalDataSource : AuthLocalDataSource {

    private val _credentials = MutableStateFlow<AuthCredentials?>(null)

    /**
     * Tracks the number of times [saveCredentials] was called.
     */
    var saveCredentialsCallCount = 0
        private set

    /**
     * Stores the last credentials passed to [saveCredentials].
     */
    var lastSavedCredentials: AuthCredentials? = null
        private set

    /**
     * Tracks the number of times [clear] was called.
     */
    var clearCallCount = 0
        private set

    override fun observeCredentials(): Flow<AuthCredentials?> = _credentials

    override suspend fun saveCredentials(creds: AuthCredentials) {
        saveCredentialsCallCount++
        lastSavedCredentials = creds
        _credentials.value = creds
    }

    override suspend fun getCredentials(): AuthCredentials? = _credentials.value

    override suspend fun clear() {
        clearCallCount++
        _credentials.value = null
    }

    /**
     * Sets the current credentials directly for testing.
     */
    fun setCredentials(creds: AuthCredentials?) {
        _credentials.value = creds
    }
}
