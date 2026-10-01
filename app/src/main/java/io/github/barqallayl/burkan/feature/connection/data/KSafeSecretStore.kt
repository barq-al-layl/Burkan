package io.github.barqallayl.burkan.feature.connection.data

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode

/**
 * Secrets encrypted with a key held in the Android Keystore. The file, `connection`, is excluded from backup and
 * device transfer in `data_extraction_rules.xml`.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class KSafeSecretStore(application: Application) : SecretStore {

    private val safe by lazy { KSafe(application, fileName = "connection") }

    override suspend fun get(name: String): String? = safe.get(name, "").ifEmpty { null }

    override suspend fun put(name: String, value: String) {
        safe.put(name, value, KSafeWriteMode.Encrypted())
    }
}
