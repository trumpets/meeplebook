package app.meeplebook.core.network.token

import app.meeplebook.BuildConfig
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Provides the BGG bearer token by deobfuscating the BuildConfig values.
 * The token is stored in obfuscated form (XOR with a random or env-provided key) in BuildConfig
 * to make it harder to extract via APK decompilation.
 */
class TokenProviderImpl @Inject constructor() : TokenProvider {

    /**
     * Cached token value to avoid repeated deobfuscation on every HTTP request.
     * Computed lazily on first access.
     */
    private val cachedToken: String by lazy {
        val obfuscated = BuildConfig.BGG_TOKEN_OBFUSCATED
        val key = BuildConfig.BGG_TOKEN_KEY

        if (obfuscated.isEmpty() || key.isEmpty()) {
            ""
        } else {
            deobfuscate(obfuscated, key)
        }
    }

    /**
     * Retrieves the BGG bearer token.
     * The token is deobfuscated once and cached for subsequent calls.
     * Returns empty string if token is not configured.
     */
    override fun getBggToken(): String {
        return cachedToken
    }

    /**
     * Obfuscates a token using XOR with a random key (for testing purposes).
     * 
     * This method is the single source of truth for obfuscation logic testing.
     * The build-time obfuscation in build.gradle.kts uses a matching algorithm
     * with support for deterministic keys via BGG_OBFUSCATION_KEY environment variable.
     * Internal visibility to allow testing without duplication.
     *
     * @param token The plaintext token to obfuscate
     * @return A pair of (obfuscatedToken, key) as hex strings
     */
    internal fun obfuscate(token: String): Pair<String, String> {
        if (token.isEmpty()) return "" to ""
        val tokenBytes = token.toByteArray(Charsets.UTF_8)

        // Generate random key for testing
        val keyBytes = ByteArray(tokenBytes.size).also { SecureRandom().nextBytes(it) }

        val obfuscatedBytes = ByteArray(tokenBytes.size)
        for (i in tokenBytes.indices) {
            obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }
        return obfuscatedBytes.joinToString("") { "%02x".format(it) } to
               keyBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Deobfuscates a hex-encoded XOR'd string using the provided key.
     * Internal visibility to allow testing without duplication.
     *
     * @return The deobfuscated string, or empty string if deobfuscation fails
     *         (e.g., invalid hex format, length mismatch, or encoding errors)
     */
    internal fun deobfuscate(obfuscatedHex: String, keyHex: String): String {
        return try {
            val obfuscatedBytes = obfuscatedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val keyBytes = keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

            // Ensure both arrays have the same length
            if (obfuscatedBytes.size != keyBytes.size) {
                return ""
            }

            val originalBytes = ByteArray(obfuscatedBytes.size)
            for (i in obfuscatedBytes.indices) {
                originalBytes[i] = (obfuscatedBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
            }

            String(originalBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
