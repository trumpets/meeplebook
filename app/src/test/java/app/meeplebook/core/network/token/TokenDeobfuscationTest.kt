package app.meeplebook.core.network.token

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the obfuscation/deobfuscation logic used by TokenProvider.
 * Uses TokenProvider methods directly to avoid code duplication.
 * The same obfuscation logic is shared with buildSrc/TokenObfuscator.kt for build-time use.
 */
class TokenDeobfuscationTest {

    @Test
    fun `deobfuscate correctly reverses XOR obfuscation`() {
        val originalToken = "test_token_123"

        // Use TokenProvider.obfuscate() - the single source of truth
        val (obfuscatedHex, keyHex) = TokenProvider.obfuscate(originalToken)

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

        val (obfuscatedHex, keyHex) = TokenProvider.obfuscate(originalToken)

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

        val (obfuscatedHex, keyHex) = TokenProvider.obfuscate(originalToken)
        val result = TokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `obfuscate handles empty string`() {
        val (obfuscated, key) = TokenProvider.obfuscate("")
        assertEquals("", obfuscated)
        assertEquals("", key)
    }

    @Test
    fun `obfuscate produces deterministic output`() {
        val token = "test_token"
        val (obfuscated1, key1) = TokenProvider.obfuscate(token)
        val (obfuscated2, key2) = TokenProvider.obfuscate(token)

        assertEquals(obfuscated1, obfuscated2)
        assertEquals(key1, key2)
    }

    @Test
    fun `deobfuscate returns empty string for invalid hex characters`() {
        val result = TokenProvider.deobfuscate("gg11zz", "aabbcc")
        assertEquals("", result)
    }

    @Test
    fun `deobfuscate returns empty string for odd-length hex string`() {
        val result = TokenProvider.deobfuscate("abc", "def")
        assertEquals("", result)
    }
}
