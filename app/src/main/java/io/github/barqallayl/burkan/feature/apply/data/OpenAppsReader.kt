package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.Either
import arrow.core.raise.either
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.core.shell.parseProcessRenderers

/**
 * Finds the apps that are open and behind: still drawing with the renderer they started with, after the property
 * has moved on. A process keeps its renderer for as long as it lives, so after an apply on a phone that has been in
 * use, whatever was open stays on OpenGL until it is restarted; and after going back to OpenGL, on Vulkan.
 * Read-only.
 */
class OpenAppsReader(private val shell: ShellExecutor) {

    /**
     * The packages with a process that draws with a renderer other than [target]. Empty when [target] is unknown:
     * there is then nothing to be behind. A process that has drawn nothing reports no renderer and is not counted.
     */
    suspend fun notOn(target: Renderer): Either<ShellError, Set<PackageName>> = either {
        if (target == Renderer.Unknown) return@either emptySet()
        parseProcessRenderers(shell.run(ShellCommands.gfxInfoAll()).bind().stdout)
            .filterValues { it != target && it != Renderer.Unknown }
            // A process is named after its package, with `:name` added for any but the main one. The system's own
            // process is named `system`: an app's package always has a dot, and that never does.
            .keys.mapNotNullTo(mutableSetOf()) { name ->
                name.substringBefore(':').takeIf { '.' in it }?.let(PackageName::parse)
            }
    }

    /**
     * Those of them a run would restart: not the system surfaces, which have rows and restarts of their own, and
     * not what no run force-stops (`FullApplyPlan.kept`). [surfaces] is the status just read.
     */
    suspend fun behind(
        surfaces: Surfaces,
        userExclusions: Set<PackageName>,
        self: PackageName,
    ): Either<ShellError, Set<PackageName>> = either {
        val open = notOn(surfaces.status.newApps).bind()
        if (open.isEmpty()) return@either open
        val inputMethods = parseInputMethodPackages(shell.run(ShellCommands.listInputMethods()).bind().stdout)
        val wallpaper = parseWallpaperPackage(shell.run(ShellCommands.dumpWallpaper()).bind().stdout)
        val kept = FullApplyPlan.kept(inputMethods, surfaces.keyboard, wallpaper, userExclusions, self) +
            listOfNotNull(ShellCommands.SystemUi, surfaces.launcher)
        open.filterTo(mutableSetOf()) { FullApplyPlan.mayStop(it, kept) }
    }
}
