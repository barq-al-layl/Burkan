package io.github.barqallayl.burkan.core.di

import android.app.Application
import androidx.annotation.Keep
import dev.zacsweers.metrox.android.MetroAppComponentFactory
import dev.zacsweers.metrox.android.MetroApplication

/**
 * Metro's component factory, with one case of its own.
 *
 * Android does not always start the app with the app's own application class. For a backup or a restore it starts
 * the process in a restricted mode, with a plain [Application] and none of the app's components, so that nothing of
 * the app runs while its files are read. Metro's factory casts whatever application it is given to
 * [MetroApplication], and on the plain one that cast threw: the process died at start, and every backup of the app
 * failed with it.
 *
 * Here a plain application is created as it is and left alone. No graph is built for it and none is needed, since
 * the restricted mode starts no activity, service or receiver.
 */
@Keep
class BurkanAppComponentFactory : MetroAppComponentFactory() {

    override fun instantiateApplicationCompat(cl: ClassLoader, className: String): Application {
        val type = Class.forName(className, false, cl)
        if (MetroApplication::class.java.isAssignableFrom(type)) return super.instantiateApplicationCompat(cl, className)
        return type.asSubclass(Application::class.java).getDeclaredConstructor().newInstance()
    }
}
