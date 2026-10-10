package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommand
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.parseComponentPackage
import io.github.barqallayl.burkan.core.shell.parseSettingValue
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting

/** Reads what a plan needs from the device. Read-only: nothing here changes the device. */
class ApplyInputsReader(private val shell: ShellExecutor) {

    /** The current input method's package, or null when none is set. */
    suspend fun readKeyboard(): Either<AppError, PackageName?> = either {
        parseSettingValue(read(ShellCommands.getSetting(SettingKey.DefaultInputMethod)).bind())
            ?.let(::parseComponentPackage)
    }

    suspend fun readFull(): Either<AppError, FullApplyInputs> = either {
        // On a phone with Secure Folder, pm complains on stderr about the other user and still lists user 0's
        // packages on stdout, so only stdout counts.
        val installed = parsePackageList(read(ShellCommands.listPackages(), failOnExitCode = false).bind())
        ensure(installed.isNotEmpty()) { ApplyError.NoPackagesListed }

        FullApplyInputs(
            installed = installed,
            inputMethods = parseInputMethodPackages(read(ShellCommands.listInputMethods()).bind()),
            wallpaper = parseWallpaperPackage(read(ShellCommands.dumpWallpaper()).bind()),
            running = parseRunningPackages(read(ShellCommands.dumpProcesses()).bind(), installed.toSet()),
            widgets = parseWidgetPackages(read(ShellCommands.dumpAppWidgets()).bind()),
            keyboard = readKeyboard().bind(),
            captured = RestoredSetting.entries.associateWith { setting ->
                parseSettingValue(read(ShellCommands.getSetting(setting.key)).bind())
            },
        )
    }

    private suspend fun read(command: ShellCommand, failOnExitCode: Boolean = true): Either<AppError, String> =
        either {
            val result = shell.run(command).bind()
            ensure(!failOnExitCode || result.exitCode == 0) {
                ApplyError.CommandFailed(result.exitCode, result.stderr)
            }
            result.stdout
        }
}
