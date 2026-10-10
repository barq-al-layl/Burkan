package io.github.barqallayl.burkan.core.di

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.annotation.Keep
import dev.zacsweers.metrox.android.MetroAppComponentFactory
import dev.zacsweers.metrox.android.MetroApplication
import io.github.barqallayl.burkan.feature.crash.ui.CrashActivity

/**
 * Metro's component factory, with two cases of its own.
 *
 * Android does not always start the app with the app's own application class. For a backup or a restore it starts
 * the process in a restricted mode, with a plain [Application] and none of the app's components, so that nothing of
 * the app runs while its files are read. Metro's factory casts whatever application it is given to
 * [MetroApplication], and on the plain one that cast threw: the process died at start, and every backup of the app
 * failed with it.
 *
 * Here a plain application is created as it is and left alone. No graph is built for it and none is needed, since
 * the restricted mode starts no activity, service or receiver.
 *
 * The crash screen is the other: it is created as it is, without the graph being built to look it up, because what
 * it reports may be the graph failing to build.
 */
@Keep
class BurkanAppComponentFactory : MetroAppComponentFactory() {

    override fun instantiateActivityCompat(cl: ClassLoader, className: String, intent: Intent?): Activity {
        if (className == CrashActivity::class.java.name) return CrashActivity()
        return super.instantiateActivityCompat(cl, className, intent)
    }

    override fun instantiateApplicationCompat(cl: ClassLoader, className: String): Application {
        val type = Class.forName(className, false, cl)
        if (MetroApplication::class.java.isAssignableFrom(type)) return super.instantiateApplicationCompat(cl, className)
        return type.asSubclass(Application::class.java).getDeclaredConstructor().newInstance()
    }
}
