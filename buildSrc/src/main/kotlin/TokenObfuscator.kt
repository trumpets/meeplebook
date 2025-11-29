import java.security.MessageDigest

/**
 * Shared obfuscation logic used by build.gradle.kts at build time.
 * 
 * IMPORTANT: This implementation MUST stay in sync with TokenProvider.obfuscate() in:
 * app/src/main/java/app/meeplebook/core/network/token/TokenProvider.kt
 * 
 * The TokenProvider version is the source of truth and is tested by unit tests.
 * This buildSrc version exists because build scripts cannot access main source code.
 */
object TokenObfuscator {

    /**
     * Obfuscates a token using XOR with a deterministic key derived from the token itself.
     * Uses SHA-256 hash of the token as the key to ensure consistent obfuscation across builds.
     * This prevents unnecessary recompilation when the token hasn't changed.
     *
     * @param token The plaintext token to obfuscate
     * @return A pair of (obfuscatedToken, key) as hex strings
     */
    fun obfuscate(token: String): Pair<String, String> {
        if (token.isEmpty()) return "" to ""
        val tokenBytes = token.toByteArray(Charsets.UTF_8)

        // Derive key deterministically from token using SHA-256
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(tokenBytes)

        // Expand hash to match token length if needed
        val keyBytes = ByteArray(tokenBytes.size) { i -> hashBytes[i % hashBytes.size] }

        val obfuscatedBytes = ByteArray(tokenBytes.size)
        for (i in tokenBytes.indices) {
            obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }
        return obfuscatedBytes.joinToString("") { "%02x".format(it) } to
               keyBytes.joinToString("") { "%02x".format(it) }
    }
}
