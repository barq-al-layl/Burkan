package io.github.barqallayl.burkan.feature.status.data

import arrow.core.Either
import arrow.core.flatten
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.RendererReader
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess

/** Reads which renderer is in use. It needs a connection, so it can switch wireless debugging on and off. */
@Inject
class StatusRepository(private val shellAccess: ShellAccess) {

    suspend fun read(): Either<AppError, RendererStatus> =
        shellAccess.withShell { shell -> RendererReader(shell).read().map { it.status } }.flatten()

    /** Closes the connection now rather than keeping it for a next read. */
    suspend fun release() = shellAccess.release()
}
