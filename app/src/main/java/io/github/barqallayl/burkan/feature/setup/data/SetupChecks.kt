package io.github.barqallayl.burkan.feature.setup.data

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.shell.PackageName

/** The parts of setup only the system can answer. */
interface SetupChecks {
    fun notificationsAllowed(): Boolean

    fun developerOptionsEnabled(): Boolean

    fun batteryExempt(): Boolean

    val deviceModel: String

    val ownPackage: PackageName
}

@Inject
@ContributesBinding(AppScope::class)
class SystemSetupChecks(private val application: Application) : SetupChecks {

    override fun notificationsAllowed(): Boolean =
        application.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun developerOptionsEnabled(): Boolean =
        Settings.Global.getInt(application.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

    override fun batteryExempt(): Boolean =
        application.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(application.packageName)

    override val deviceModel: String = Build.MODEL

    override val ownPackage: PackageName = PackageName.known(application.packageName)
}
