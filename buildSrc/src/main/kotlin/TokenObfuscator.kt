import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Shared obfuscation logic used by build.gradle.kts at build time.
 * 
 * IMPORTANT: This implementation MUST stay in sync with TokenProvider.obfuscate() in:
 * app/src/main/java/app/meeplebook/core/network/token/TokenProvider.kt
 * 
 * The TokenProvider version is the source of truth and is tested by unit tests.
 * This buildSrc version exists because build scripts cannot access main source code.
 * 
 * Security note: By default, uses a random key for stronger obfuscation. This triggers
 * recompilation on every build. For deterministic builds, set BGG_OBFUSCATION_KEY
 * environment variable to a constant value.
 */
object TokenObfuscator {

    /**
     * Gets or generates the obfuscation key.
     * If BGG_OBFUSCATION_KEY environment variable is set, uses that for deterministic builds.
     * Otherwise, generates a random key (stronger security but triggers recompilation).
     */
    private fun getOrGenerateKey(tokenLength: Int): ByteArray {
        val envKey = System.getenv("BGG_OBFUSCATION_KEY")
        return if (envKey != null && envKey.isNotEmpty()) {
            // Use provided key, expand with SHA-256 if needed to match token length
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(envKey.toByteArray(Charsets.UTF_8))
            ByteArray(tokenLength) { i -> hashBytes[i % hashBytes.size] }
        } else {
            // Generate random key for stronger obfuscation
            ByteArray(tokenLength).also { SecureRandom().nextBytes(it) }
        }
    }

    /**
     * Obfuscates a token using XOR with a random or environment-provided key.
     *
     * @param token The plaintext token to obfuscate
     * @return A pair of (obfuscatedToken, key) as hex strings
     */
    fun obfuscate(token: String): Pair<String, String> {
        if (token.isEmpty()) return "" to ""
        val tokenBytes = token.toByteArray(Charsets.UTF_8)
        val keyBytes = getOrGenerateKey(tokenBytes.size)

        val obfuscatedBytes = ByteArray(tokenBytes.size)
        for (i in tokenBytes.indices) {
            obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }
        return obfuscatedBytes.joinToString("") { "%02x".format(it) } to
               keyBytes.joinToString("") { "%02x".format(it) }
    }
}
