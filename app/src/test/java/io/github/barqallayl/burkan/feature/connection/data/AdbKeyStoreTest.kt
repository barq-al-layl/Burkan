package io.github.barqallayl.burkan.feature.connection.data

import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdbKeyStoreTest {

    private val secrets = MemorySecretStore()

    @Test
    fun `the key is a 2048-bit RSA key in a self-signed certificate`() {
        val keys = generateAdbKeys()

        val publicKey = keys.certificate.publicKey as RSAPublicKey
        assertEquals(2048, publicKey.modulus.bitLength())
        assertEquals((keys.privateKey as RSAPrivateCrtKey).modulus, publicKey.modulus)
        keys.certificate.verify(publicKey)
        assertEquals(keys.certificate.issuerX500Principal, keys.certificate.subjectX500Principal)
    }

    @Test
    fun `the key is generated once and then reused`() = runTest {
        val store = AdbKeyStore(secrets)

        val first = store.keys()
        val second = store.keys()

        assertEquals(first.certificate, second.certificate)
        assertEquals(2, secrets.writes)
    }

    @Test
    fun `a stored key that cannot be read is replaced by a new one`() = runTest {
        AdbKeyStore(secrets).keys()
        listOf("not base64 !", "AAAA").forEach { damaged ->
            secrets.put("adb_private_key", damaged)
            val writes = secrets.writes

            val keys = AdbKeyStore(secrets).keys()

            assertEquals(2048, (keys.certificate.publicKey as RSAPublicKey).modulus.bitLength())
            assertEquals(writes + 2, secrets.writes, "the new key is stored in place of \"$damaged\"")
            // And it is the one read back from then on.
            assertEquals(keys.certificate, AdbKeyStore(secrets).keys().certificate)
        }
    }

    @Test
    fun `a restarted app reads the same key back`() = runTest {
        val before = AdbKeyStore(secrets).keys()

        val after = AdbKeyStore(secrets).keys()

        assertEquals(before.certificate, after.certificate)
        assertTrue(before.privateKey.encoded.contentEquals(after.privateKey.encoded))
        assertEquals(2, secrets.writes)
    }

    @Test
    fun `the key never appears in its text form`() {
        val keys = generateAdbKeys()

        assertFalse(keys.toString().contains(keys.privateKey.toString()))
        assertEquals("AdbKeys(CN=Burkan)", keys.toString())
    }

    private class MemorySecretStore : SecretStore {
        private val values = mutableMapOf<String, String>()
        var writes = 0

        override suspend fun get(name: String): String? = values[name]

        override suspend fun put(name: String, value: String) {
            values[name] = value
            writes++
        }
    }
}
