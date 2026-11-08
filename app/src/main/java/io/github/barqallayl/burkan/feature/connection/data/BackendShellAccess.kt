package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import kotlinx.coroutines.flow.first
import kotlin.time.Duration

/**
 * The [ShellAccess] the rest of the app uses: the way to a shell the user chose in Settings, looked up each time a
 * block starts. A choice made between two runs takes effect at the next one; a run in progress keeps the shell it
 * began with.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class BackendShellAccess(
    private val settings: SettingsStorage,
    private val wirelessDebugging: AdbShellAccess,
    private val shizuku: ShizukuShellAccess,
) : ShellAccess {

    override suspend fun <T> withShell(
        settle: Duration,
        patience: Duration,
        block: suspend (ShellExecutor) -> T,
    ): Either<AppError, T> = when (settings.shellBackend.first()) {
        ShellBackend.WirelessDebugging -> wirelessDebugging.withShell(settle, patience, block)
        ShellBackend.Shizuku -> shizuku.withShell(settle, patience, block)
    }

    /** Both: a shell kept from before the choice changed is closed too. */
    override suspend fun release() {
        wirelessDebugging.release()
        shizuku.release()
    }
}
