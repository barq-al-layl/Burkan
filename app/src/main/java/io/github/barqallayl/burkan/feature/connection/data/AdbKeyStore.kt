package io.github.barqallayl.burkan.feature.connection.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date

/** The app's identity towards adbd: what pairing authorises, and what every TLS connection presents. */
class AdbKeys(val privateKey: PrivateKey, val certificate: X509Certificate) {
    // The key must never reach a log.
    override fun toString(): String = "AdbKeys(${certificate.subjectX500Principal})"
}

/** Somewhere encrypted to keep secrets. */
interface SecretStore {
    suspend fun get(name: String): String?

    suspend fun put(name: String, value: String)
}

/**
 * Generates the key pair on first use and keeps it in [SecretStore]. Generating a new one would undo the pairing, so
 * it happens only when nothing is stored.
 */
@Inject
@SingleIn(AppScope::class)
class AdbKeyStore(private val secrets: SecretStore) {

    private val mutex = Mutex()
    private var cached: AdbKeys? = null

    suspend fun keys(): AdbKeys = mutex.withLock {
        cached ?: (load() ?: generateAdbKeys().also { save(it) }).also { cached = it }
    }

    private suspend fun load(): AdbKeys? {
        val key = secrets.get(PRIVATE_KEY) ?: return null
        val certificate = secrets.get(CERTIFICATE) ?: return null
        return AdbKeys(
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(decode(key))),
            certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(decode(certificate).inputStream()) as X509Certificate,
        )
    }

    private suspend fun save(keys: AdbKeys) {
        secrets.put(PRIVATE_KEY, encode(keys.privateKey.encoded))
        secrets.put(CERTIFICATE, encode(keys.certificate.encoded))
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val PRIVATE_KEY = "adb_private_key"
        const val CERTIFICATE = "adb_certificate"
    }
}

/** A 2048-bit RSA key, the size adbd expects, and a self-signed certificate around its public half. */
fun generateAdbKeys(now: Instant = Instant.now()): AdbKeys {
    val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val name = X500Name("CN=Burkan")
    val holder = JcaX509v3CertificateBuilder(
        name,
        BigInteger.valueOf(now.epochSecond),
        Date.from(now),
        Date.from(now.plus(CERTIFICATE_DAYS, ChronoUnit.DAYS)),
        name,
        pair.public,
    ).build(JcaContentSignerBuilder("SHA256withRSA").build(pair.private))
    return AdbKeys(pair.private, JcaX509CertificateConverter().getCertificate(holder))
}

private const val CERTIFICATE_DAYS = 365L * 30
