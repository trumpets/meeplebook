package app.meeplebook.core.network.token

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the deobfuscation logic used by TokenProvider.
 * Note: We can't directly test TokenProvider.getBggToken() in unit tests
 * because it depends on BuildConfig which is generated at compile time.
 * Instead, we test the deobfuscation algorithm independently.
 */
class TokenDeobfuscationTest {

    @Test
    fun `deobfuscate correctly reverses XOR obfuscation`() {
        val originalToken = "test_token_123"

        // Simulate obfuscation (same logic as in build.gradle.kts)
        val (obfuscatedHex, keyHex) = obfuscateToken(originalToken)

        // Deobfuscate
        val result = deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `deobfuscate handles empty strings`() {
        assertEquals("", deobfuscate("", ""))
    }

    @Test
    fun `deobfuscate handles special characters`() {
        val originalToken = "token!@#\$%^&*()_+-=[]{}|;':\",./<>?"

        val (obfuscatedHex, keyHex) = obfuscateToken(originalToken)

        val result = deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `deobfuscate returns empty string when lengths mismatch`() {
        // Mismatched lengths should return empty string
        val result = deobfuscate("aabbcc", "1122")
        assertEquals("", result)
    }

    @Test
    fun `deobfuscate handles long tokens`() {
        val originalToken = "this_is_a_very_long_bearer_token_that_might_be_used_in_production_12345678901234567890"

        val (obfuscatedHex, keyHex) = obfuscateToken(originalToken)
        val result = deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    /**
     * Simulates the obfuscation logic from build.gradle.kts for testing purposes.
     */
    private fun obfuscateToken(token: String): Pair<String, String> {
        if (token.isEmpty()) return "" to ""

        val tokenBytes = token.toByteArray(Charsets.UTF_8)
        // Use a deterministic key for testing (in production, random key is used)
        val keyBytes = ByteArray(tokenBytes.size) { i -> ((i * 17 + 5) and 0xFF).toByte() }

        val obfuscatedBytes = ByteArray(tokenBytes.size)
        for (i in tokenBytes.indices) {
            obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }

        return obfuscatedBytes.joinToString("") { "%02x".format(it) } to
               keyBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Deobfuscates a hex-encoded XOR'd string using the provided key.
     * This is a copy of the logic from TokenProvider for testing purposes.
     */
    private fun deobfuscate(obfuscatedHex: String, keyHex: String): String {
        if (obfuscatedHex.isEmpty() || keyHex.isEmpty()) {
            return ""
        }

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

        return String(originalBytes, Charsets.UTF_8)
    }
}
