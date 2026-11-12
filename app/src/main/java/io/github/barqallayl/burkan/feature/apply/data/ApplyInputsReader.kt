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

/**
 * Reads what a plan needs from the device. Read-only: nothing here changes the device.
 *
 * The settings a run puts back afterwards are read by the app itself, through [captured], not over the shell: see
 * [CapturedSettings] for why.
 */
class ApplyInputsReader(private val shell: ShellExecutor, private val captured: CapturedSettings) {

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
            captured = RestoredSetting.entries.associateWith { setting -> capture(setting).bind() },
        )
    }

    /**
     * What [setting] holds now, or null when it is unset. The app's own read is the answer. Where it gives none,
     * because Android refused or the key reads as unset to an app, the shell is asked instead, and believed only
     * for a value the setting could hold. The accessibility services are never taken from the shell: that is the
     * one value worth lying about.
     */
    private suspend fun capture(setting: RestoredSetting): Either<AppError, String?> = either {
        val own = (captured.read(setting) as? SettingRead.Value)?.value?.takeIf { it.isNotEmpty() }
        when {
            own != null -> own
            setting == RestoredSetting.AccessibilityServices -> null
            else -> parseSettingValue(read(ShellCommands.getSetting(setting.key)).bind())?.takeIf(setting::accepts)
        }
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
