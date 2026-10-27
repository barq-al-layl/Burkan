package io.github.barqallayl.burkan.feature.status.data

import arrow.core.Either
import arrow.core.flatten
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.RendererReader
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.data.OpenAppsReader
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import io.github.barqallayl.burkan.feature.settings.data.InstalledApps
import io.github.barqallayl.burkan.feature.status.model.BehindApp
import kotlinx.coroutines.flow.first

/** Reads which renderer is in use. It needs a connection, so it can switch wireless debugging on and off. */
@Inject
class StatusRepository(
    private val shellAccess: ShellAccess,
    private val installedApps: InstalledApps,
    private val settings: SettingsStorage,
    private val wirelessDebugging: WirelessDebugging,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
) {
    /** What [read] last found, which says what the open apps are compared with and which of them are surfaces. */
    private var surfaces: Surfaces? = null

    suspend fun read(): Either<AppError, RendererStatus> =
        shellAccess.withShell { shell -> RendererReader(shell).read().onRight { surfaces = it }.map { it.status } }
            .flatten()
            .onLeft { surfaces = null }

    /**
     * The open apps still on the renderer new apps no longer get, by name, for the status [read] last. It asks
     * every running process, which takes a second or two more than the status does, so it is asked for after the
     * status is shown. Null when there is no status to go by, or the phone could not be asked.
     */
    suspend fun readBehind(): List<BehindApp>? {
        val surfaces = surfaces ?: return null
        val userExclusions = settings.userExclusions.first()
        val behind = shellAccess.withShell { shell -> OpenAppsReader(shell).behind(surfaces, userExclusions, ownPackage) }
            .flatten()
            .getOrNull() ?: return null
        return describe(behind)
    }

    /**
     * [packages] as a person knows them, in the order of their names. A name that is no installed package is left
     * out: some processes are named after something other than their package.
     */
    suspend fun describe(packages: Set<PackageName>): List<BehindApp> =
        installedApps.describe(packages.toList())
            .mapNotNull { app -> app.label?.let { BehindApp(app.packageName, it) } }
            .sortedBy { it.label.lowercase() }

    /** Adds [packageName] to the apps no run restarts. */
    suspend fun keep(packageName: PackageName) {
        settings.setUserExclusions(settings.userExclusions.first() + packageName)
    }

    /** Closes the connection now rather than keeping it for a next read. */
    suspend fun release() = shellAccess.release()

    /** Whether Android will forget the pairing if Burkan does not connect for a while. See [WirelessDebugging]. */
    fun pairingCanLapse(): Boolean = wirelessDebugging.forgetsPairings()
}
