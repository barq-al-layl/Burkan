package io.github.barqallayl.burkan.feature.settings.data

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The apps on the phone. */
interface InstalledApps {
    /** The apps a person would recognise: those with a launcher icon. */
    suspend fun launchable(): Either<SettingsError, List<ExcludableApp>>

    /** [packages] with their labels; a package that is not installed has none. */
    suspend fun describe(packages: List<PackageName>): List<ExcludableApp>
}

/** Needs `QUERY_ALL_PACKAGES`: without it Android hides most apps from the list. */
@Inject
@ContributesBinding(AppScope::class)
class PackageManagerInstalledApps(private val application: Application) : InstalledApps {

    private val packageManager: PackageManager get() = application.packageManager

    override suspend fun launchable(): Either<SettingsError, List<ExcludableApp>> = withContext(Dispatchers.IO) {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = try {
            packageManager.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
        } catch (e: RuntimeException) {
            // With many apps the answer can outgrow the binder transaction and arrive as an exception.
            Logger.w(e) { "Listing launchable apps failed" }
            return@withContext SettingsError.AppsNotListed.left()
        }
        activities
            .filter { it.activityInfo.packageName != application.packageName }
            .mapNotNull { info ->
                val isSystem = info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                PackageName.parse(info.activityInfo.packageName)
                    ?.let { ExcludableApp(it, info.loadLabel(packageManager).toString(), isSystem) }
            }
            .distinctBy { it.packageName }
            .right()
    }

    override suspend fun describe(packages: List<PackageName>): List<ExcludableApp> = withContext(Dispatchers.IO) {
        packages.map { packageName -> ExcludableApp(packageName, labelOf(packageName)) }
    }

    private fun labelOf(packageName: PackageName): String? = try {
        val info = packageManager.getApplicationInfo(packageName.value, PackageManager.ApplicationInfoFlags.of(0))
        packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
