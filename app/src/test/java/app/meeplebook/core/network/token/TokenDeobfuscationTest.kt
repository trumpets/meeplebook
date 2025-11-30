package app.meeplebook.core.network.token

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for the obfuscation/deobfuscation logic used by TokenProviderImpl.
 * Uses TokenProviderImpl methods directly to avoid code duplication.
 * The build-time obfuscation uses a matching algorithm with optional deterministic keys
 * via BGG_OBFUSCATION_KEY environment variable.
 */
class TokenDeobfuscationTest {

    private lateinit var tokenProvider: TokenProviderImpl

    @Before
    fun setUp() {
        tokenProvider = TokenProviderImpl()
    }

    @Test
    fun `deobfuscate correctly reverses XOR obfuscation`() {
        val originalToken = "test_token_123"

        // Use TokenProviderImpl.obfuscate() - the single source of truth
        val (obfuscatedHex, keyHex) = tokenProvider.obfuscate(originalToken)

        // Deobfuscate using TokenProviderImpl
        val result = tokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `deobfuscate handles empty strings`() {
        assertEquals("", tokenProvider.deobfuscate("", ""))
    }

    @Test
    fun `deobfuscate handles special characters`() {
        val originalToken = "token!@#\$%^&*()_+-=[]{}|;':\",./<>?"

        val (obfuscatedHex, keyHex) = tokenProvider.obfuscate(originalToken)

        val result = tokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `deobfuscate returns empty string when lengths mismatch`() {
        // Mismatched lengths should return empty string
        val result = tokenProvider.deobfuscate("aabbcc", "1122")
        assertEquals("", result)
    }

    @Test
    fun `deobfuscate handles long tokens`() {
        val originalToken = "this_is_a_very_long_bearer_token_that_might_be_used_in_production_12345678901234567890"

        val (obfuscatedHex, keyHex) = tokenProvider.obfuscate(originalToken)
        val result = tokenProvider.deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
    }

    @Test
    fun `obfuscate handles empty string`() {
        val (obfuscated, key) = tokenProvider.obfuscate("")
        assertEquals("", obfuscated)
        assertEquals("", key)
    }

    @Test
    fun `obfuscate produces different output each time with random key`() {
        val token = "test_token"
        val (obfuscated1, key1) = tokenProvider.obfuscate(token)
        val (obfuscated2, key2) = tokenProvider.obfuscate(token)

        // Random keys means different obfuscated output each time
        assertNotEquals(obfuscated1, obfuscated2)
        assertNotEquals(key1, key2)
    }

    @Test
    fun `deobfuscate returns empty string for invalid hex characters`() {
        val result = tokenProvider.deobfuscate("gg11zz", "aabbcc")
        assertEquals("", result)
    }

    @Test
    fun `deobfuscate returns empty string for odd-length hex string`() {
        val result = tokenProvider.deobfuscate("abc", "def")
        assertEquals("", result)
    }
}
