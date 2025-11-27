package app.meeplebook.core.auth.local

import androidx.datastore.preferences.core.stringPreferencesKey
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.security.EncryptedPreferencesDataStore
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AuthLocalDataSourceImpl @Inject constructor(
    private val encryptedDs: EncryptedPreferencesDataStore
) : AuthLocalDataSource {

    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_PASSWORD = stringPreferencesKey("password")

    private val credentialsFlow = MutableStateFlow<AuthCredentials?>(null)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val usernameFlow = encryptedDs.getDecryptedString(KEY_USERNAME)
        val passwordFlow = encryptedDs.getDecryptedString(KEY_PASSWORD)

        combine(usernameFlow, passwordFlow) { u, p ->
            if (u != null && p != null) AuthCredentials(u, p) else null
        }
        .onEach { credentialsFlow.value = it }
        .launchIn(scope)
    }

    override fun observeCredentials(): Flow<AuthCredentials?> {
        return credentialsFlow
    }

    override suspend fun saveCredentials(creds: AuthCredentials) {
        encryptedDs.putEncryptedString(KEY_USERNAME, creds.username)
        encryptedDs.putEncryptedString(KEY_PASSWORD, creds.password)
    }

    override suspend fun getCredentials(): AuthCredentials? {
        return credentialsFlow.value
    }

    override suspend fun clear() {
        encryptedDs.clear()
    }
}