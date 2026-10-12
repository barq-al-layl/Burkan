package io.github.barqallayl.burkan.feature.settings.data

import arrow.core.Either
import arrow.core.right
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import io.github.barqallayl.burkan.feature.settings.model.SettingsError

/** Knows the apps in [labels], package name to label; any other package is not installed and has no label. */
class FakeInstalledApps(private val labels: Map<String, String> = emptyMap()) : InstalledApps {

    override suspend fun launchable(): Either<SettingsError, List<ExcludableApp>> =
        labels.map { (name, label) -> ExcludableApp(PackageName.known(name), label) }.right()

    override suspend fun describe(packages: List<PackageName>): List<ExcludableApp> =
        packages.map { ExcludableApp(it, labels[it.value]) }
}
