package io.github.barqallayl.burkan

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication
import io.github.barqallayl.burkan.feature.apply.BootReceiver
import io.github.barqallayl.burkan.feature.apply.NetworkReceiver
import kotlin.reflect.KClass

/**
 * The application screenshot tests run under. Robolectric creates the manifest's receivers on start through the
 * manifest's component factory, which asks the application for them: its plain `Application` has none to give.
 */
class RoborazziTestApplication : Application(), MetroApplication {
    override val appComponentProviders: MetroAppComponentProviders = object : MetroAppComponentProviders {
        override val activityProviders: Map<KClass<out Activity>, () -> Activity> = emptyMap()
        override val providerProviders: Map<KClass<out ContentProvider>, () -> ContentProvider> = emptyMap()

        // Robolectric creates the manifest's receivers on start, and they take constructor arguments. Here they
        // receive nothing, so stand-ins that do nothing will do.
        override val receiverProviders: Map<KClass<out BroadcastReceiver>, () -> BroadcastReceiver> =
            listOf(BootReceiver::class, NetworkReceiver::class).associateWith { { NoOpReceiver() } }
        override val serviceProviders: Map<KClass<out Service>, () -> Service> = emptyMap()
    }
}

private class NoOpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
