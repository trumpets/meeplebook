package app.meeplebook.core.network.token

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the deobfuscation logic used by TokenProvider.
 * Uses TokenProvider.deobfuscate() directly to avoid code duplication.
 */
class TokenDeobfuscationTest {

    @Test
    fun `deobfuscate correctly reverses XOR obfuscation`() {
        val originalToken = "test_token_123"

        // Simulate obfuscation (same logic as in build.gradle.kts)
        val (obfuscatedHex, keyHex) = obfuscateToken(originalToken)

        // Deobfuscate using TokenProvider
        val result = TokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `deobfuscate handles empty strings`() {
        assertEquals("", TokenProvider.deobfuscate("", ""))
    }

    @Test
    fun `deobfuscate handles special characters`() {
        val originalToken = "token!@#\$%^&*()_+-=[]{}|;':\",./<>?"

        val (obfuscatedHex, keyHex) = obfuscateToken(originalToken)

        val result = TokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `deobfuscate returns empty string when lengths mismatch`() {
        // Mismatched lengths should return empty string
        val result = TokenProvider.deobfuscate("aabbcc", "1122")
        assertEquals("", result)
    }

    @Test
    fun `deobfuscate handles long tokens`() {
        val originalToken = "this_is_a_very_long_bearer_token_that_might_be_used_in_production_12345678901234567890"

        val (obfuscatedHex, keyHex) = obfuscateToken(originalToken)
        val result = TokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    /**
     * Simulates the obfuscation logic from build.gradle.kts for testing purposes.
     * Uses SHA-256 hash of the token as the key (same as build.gradle.kts).
     */
    private fun obfuscateToken(token: String): Pair<String, String> {
        if (token.isEmpty()) return "" to ""

        val tokenBytes = token.toByteArray(Charsets.UTF_8)
        
        // Derive key deterministically from token using SHA-256 (matches build.gradle.kts)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
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
