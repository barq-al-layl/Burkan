package io.github.barqallayl.burkan.core.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.shell.PackageName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Clock

@ContributesTo(AppScope::class)
@BindingContainer
object AppBindings {

    /** Burkan's own package: never restarted by a full apply, and the one that grants itself its permission. */
    @Provides
    @Named(OWN_PACKAGE)
    fun provideOwnPackage(application: Application): PackageName = PackageName.known(application.packageName)

    /** Injected, so nothing reads the real clock in a test or a preview. */
    @Provides
    fun provideClock(): Clock = Clock.System

    /** Work that outlives the screen or service that started it, such as closing a connection a little later. */
    @Provides
    @SingleIn(AppScope::class)
    @Named(APP_SCOPE)
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The version name shown in Settings. */
    @Provides
    @Named(APP_VERSION)
    fun provideAppVersion(application: Application): String =
        application.packageManager.getPackageInfo(application.packageName, 0).versionName.orEmpty()

    const val OWN_PACKAGE = "own_package"
    const val APP_VERSION = "app_version"
    const val APP_SCOPE = "app_scope"
}
