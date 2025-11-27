package app.meeplebook.core.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EncryptedPreferencesDataStore(
    private val dataStore: DataStore<Preferences>,
    private val aead: Aead
) {

    suspend fun putEncryptedString(key: Preferences.Key<String>, value: String?) {
        dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(key)
            } else {
                val encryptedBytes = aead.encrypt(value.toByteArray(), null)
                prefs[key] = encryptedBytes.toHexString()
            }
        }
    }

    fun getDecryptedString(key: Preferences.Key<String>): Flow<String?> {
        return dataStore.data.map { prefs ->
            val hex = prefs[key] ?: return@map null
            val encryptedBytes = hex.hexToByteArray()
            val decryptedBytes = aead.decrypt(encryptedBytes, null)
            String(decryptedBytes)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    // Helpers
    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}