package io.github.barqallayl.burkan.feature.settings.data

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The apps a person would recognise: those with a launcher icon. */
interface InstalledApps {
    suspend fun launchable(): List<ExcludableApp>
}

/** Needs `QUERY_ALL_PACKAGES`: without it Android hides most apps from the list. */
@Inject
@ContributesBinding(AppScope::class)
class PackageManagerInstalledApps(private val application: Application) : InstalledApps {

    override suspend fun launchable(): List<ExcludableApp> = withContext(Dispatchers.IO) {
        val packageManager = application.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
            .filter { it.activityInfo.packageName != application.packageName }
            .mapNotNull { info ->
                PackageName.parse(info.activityInfo.packageName)
                    ?.let { ExcludableApp(it, info.loadLabel(packageManager).toString()) }
            }
            .distinctBy { it.packageName }
    }
}
