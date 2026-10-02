package io.github.barqallayl.burkan.core.shell

import arrow.core.Either
import arrow.core.raise.either
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus

/**
 * What the phone is rendering with, and the packages the status is about: the home app and the keyboard, each null
 * when the phone does not name one.
 */
data class Surfaces(val status: RendererStatus, val launcher: PackageName?, val keyboard: PackageName?)

/** Reads the renderer property and what each system surface runs with. Read-only. */
class RendererReader(private val shell: ShellExecutor) {

    suspend fun read(): Either<ShellError, Surfaces> = either {
        val property = shell.run(ShellCommands.getRenderer()).bind()
        val launcher = homeApp().bind()
        val keyboard = shell.run(ShellCommands.getSetting(SettingKey.DefaultInputMethod)).bind()
            .stdout.let(::parseSettingValue)?.let(::parseComponentPackage)
        Surfaces(
            status = RendererStatus(
                newApps = rendererFromProperty(property.stdout),
                systemUi = rendererOf(ShellCommands.SystemUi).bind(),
                launcher = launcher?.let { rendererOf(it).bind() } ?: Renderer.Unknown,
                keyboard = keyboard?.let { rendererOf(it).bind() } ?: Renderer.Unknown,
            ),
            launcher = launcher,
            keyboard = keyboard,
        )
    }

    /** What the running process uses. A process that is not running has no pipeline to report: unknown. */
    suspend fun rendererOf(packageName: PackageName): Either<ShellError, Renderer> = either {
        val result = shell.run(ShellCommands.gfxInfo(packageName)).bind()
        if (result.exitCode == 0) parseRenderer(result.stdout) else Renderer.Unknown
    }

    /**
     * The user's home app, which need not be Samsung's: what the home intent resolves to, or else the home role's
     * holder.
     */
    private suspend fun homeApp(): Either<ShellError, PackageName?> = either {
        val resolved = shell.run(ShellCommands.resolveHomeActivity()).bind()
        val home = if (resolved.exitCode == 0) parseHomeActivity(resolved.stdout) else null
        home ?: shell.run(ShellCommands.homeRoleHolders()).bind()
            .takeIf { it.exitCode == 0 }?.stdout?.let(::parseRoleHolders)
    }
}
