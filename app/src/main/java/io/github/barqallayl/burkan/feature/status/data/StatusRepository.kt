package io.github.barqallayl.burkan.feature.status.data

import arrow.core.Either
import arrow.core.flatten
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.parseComponentPackage
import io.github.barqallayl.burkan.core.shell.parseRenderer
import io.github.barqallayl.burkan.core.shell.parseSettingValue
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.status.model.RendererStatus
import io.github.barqallayl.burkan.feature.status.model.rendererFromProperty

/** Reads which renderer is in use. It needs a connection, so it can switch wireless debugging on and off. */
@Inject
class StatusRepository(private val shellAccess: ShellAccess) {

    suspend fun read(): Either<AppError, RendererStatus> = shellAccess.withShell { shell ->
        either {
            val property = shell.run(ShellCommands.getRenderer()).bind()
            val keyboard = shell.run(ShellCommands.getSetting(SettingKey.DefaultInputMethod)).bind()
                .stdout.let(::parseSettingValue)?.let(::parseComponentPackage)
            RendererStatus(
                newApps = rendererFromProperty(property.stdout),
                systemUi = rendererOf(shell, ShellCommands.SystemUi),
                launcher = rendererOf(shell, ShellCommands.Launcher),
                keyboard = keyboard?.let { rendererOf(shell, it) } ?: Renderer.Unknown,
            )
        }
    }.flatten()

    /** What the running process uses. A process that is not running has no pipeline to report: unknown. */
    private suspend fun Raise<AppError>.rendererOf(shell: ShellExecutor, packageName: PackageName): Renderer {
        val result = shell.run(ShellCommands.gfxInfo(packageName)).bind()
        return if (result.exitCode == 0) parseRenderer(result.stdout) else Renderer.Unknown
    }
}
