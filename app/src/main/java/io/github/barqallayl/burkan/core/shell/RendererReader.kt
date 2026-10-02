package io.github.barqallayl.burkan.core.shell

import arrow.core.Either
import arrow.core.raise.either
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus

/** What the phone is rendering with, and the keyboard the status is about. */
data class Surfaces(val status: RendererStatus, val keyboard: PackageName?)

/** Reads the renderer property and what each system surface runs with. Read-only. */
class RendererReader(private val shell: ShellExecutor) {

    suspend fun read(): Either<ShellError, Surfaces> = either {
        val property = shell.run(ShellCommands.getRenderer()).bind()
        val keyboard = shell.run(ShellCommands.getSetting(SettingKey.DefaultInputMethod)).bind()
            .stdout.let(::parseSettingValue)?.let(::parseComponentPackage)
        Surfaces(
            status = RendererStatus(
                newApps = rendererFromProperty(property.stdout),
                systemUi = rendererOf(ShellCommands.SystemUi).bind(),
                launcher = rendererOf(ShellCommands.Launcher).bind(),
                keyboard = keyboard?.let { rendererOf(it).bind() } ?: Renderer.Unknown,
            ),
            keyboard = keyboard,
        )
    }

    /** What the running process uses. A process that is not running has no pipeline to report: unknown. */
    suspend fun rendererOf(packageName: PackageName): Either<ShellError, Renderer> = either {
        val result = shell.run(ShellCommands.gfxInfo(packageName)).bind()
        if (result.exitCode == 0) parseRenderer(result.stdout) else Renderer.Unknown
    }
}
