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
        val tokenBytes = originalToken.toByteArray(Charsets.UTF_8)
        val keyBytes = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(),
            0xDE.toByte(), 0xF0.toByte(), 0x11, 0x22, 0x33, 0x44, 0x55, 0x66)

        val obfuscatedBytes = ByteArray(tokenBytes.size)
        for (i in tokenBytes.indices) {
            obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }

        val obfuscatedHex = obfuscatedBytes.joinToString("") { "%02x".format(it) }
        val keyHex = keyBytes.joinToString("") { "%02x".format(it) }

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

        val tokenBytes = originalToken.toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(tokenBytes.size) { i -> (i * 17 + 5).toByte() }

        val obfuscatedBytes = ByteArray(tokenBytes.size)
        for (i in tokenBytes.indices) {
            obfuscatedBytes[i] = (tokenBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }

        val obfuscatedHex = obfuscatedBytes.joinToString("") { "%02x".format(it) }
        val keyHex = keyBytes.joinToString("") { "%02x".format(it) }

        val result = deobfuscate(obfuscatedHex, keyHex)

        assertEquals(originalToken, result)
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

        val originalBytes = ByteArray(obfuscatedBytes.size)
        for (i in obfuscatedBytes.indices) {
            originalBytes[i] = (obfuscatedBytes[i].toInt() xor keyBytes[i].toInt()).toByte()
        }

        return String(originalBytes, Charsets.UTF_8)
    }
}
