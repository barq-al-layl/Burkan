package io.github.barqallayl.burkan.core.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import io.github.barqallayl.burkan.core.shell.PackageName
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

    const val OWN_PACKAGE = "own_package"
}
