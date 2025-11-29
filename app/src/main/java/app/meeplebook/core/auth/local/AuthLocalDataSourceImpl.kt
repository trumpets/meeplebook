package app.meeplebook.core.auth.local

import androidx.datastore.preferences.core.stringPreferencesKey
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.security.EncryptedPreferencesDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class AuthLocalDataSourceImpl @Inject constructor(
    private val encryptedDs: EncryptedPreferencesDataStore
) : AuthLocalDataSource {

    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_PASSWORD = stringPreferencesKey("password")

    private val credentialsFlow: Flow<AuthCredentials?> =
        encryptedDs.getDecryptedString(KEY_USERNAME)
            .combine(encryptedDs.getDecryptedString(KEY_PASSWORD)) { u, p ->
                if (u != null && p != null) AuthCredentials(u, p) else null
            }

    override fun observeCredentials(): Flow<AuthCredentials?> {
        return credentialsFlow
    }

    override suspend fun saveCredentials(creds: AuthCredentials) {
        encryptedDs.putEncryptedString(KEY_USERNAME, creds.username)
        encryptedDs.putEncryptedString(KEY_PASSWORD, creds.password)
    }

    override suspend fun getCredentials(): AuthCredentials? {
        return credentialsFlow.first()
    }

    override suspend fun clear() {
        encryptedDs.clear()
    }
}