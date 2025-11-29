package app.meeplebook.core.network.token

import app.meeplebook.BuildConfig

/**
 * Provides the BGG bearer token by deobfuscating the BuildConfig values.
 * The token is stored in obfuscated form (XOR with a random key) in BuildConfig
 * to make it harder to extract via APK decompilation.
 */
object TokenProvider {

    /**
     * Retrieves and deobfuscates the BGG bearer token.
     * Returns empty string if token is not configured.
     */
    fun getBggToken(): String {
        val obfuscated = BuildConfig.BGG_TOKEN_OBFUSCATED
        val key = BuildConfig.BGG_TOKEN_KEY

        if (obfuscated.isEmpty() || key.isEmpty()) {
            return ""
        }

        return deobfuscate(obfuscated, key)
    }

    /**
     * Deobfuscates a hex-encoded XOR'd string using the provided key.
     */
    private fun deobfuscate(obfuscatedHex: String, keyHex: String): String {
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
