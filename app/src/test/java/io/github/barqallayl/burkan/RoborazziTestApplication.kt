package io.github.barqallayl.burkan

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication
import kotlin.reflect.KClass

/**
 * The application screenshot tests run under. The manifest's `MetroAppComponentFactory` casts the application to
 * [MetroApplication], and Robolectric's plain `Application` would fail that cast before rendering anything.
 */
class RoborazziTestApplication : Application(), MetroApplication {
    override val appComponentProviders: MetroAppComponentProviders = object : MetroAppComponentProviders {
        override val activityProviders: Map<KClass<out Activity>, () -> Activity> = emptyMap()
        override val providerProviders: Map<KClass<out ContentProvider>, () -> ContentProvider> = emptyMap()
        override val receiverProviders: Map<KClass<out BroadcastReceiver>, () -> BroadcastReceiver> = emptyMap()
        override val serviceProviders: Map<KClass<out Service>, () -> Service> = emptyMap()
    }
}
