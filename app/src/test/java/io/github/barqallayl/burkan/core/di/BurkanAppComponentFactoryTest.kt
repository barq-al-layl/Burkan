package io.github.barqallayl.burkan.core.di

import android.app.Application
import dev.zacsweers.metrox.android.MetroApplication
import io.github.barqallayl.burkan.RoborazziTestApplication
import io.github.barqallayl.burkan.feature.crash.ui.CrashActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RoborazziTestApplication::class)
class BurkanAppComponentFactoryTest {

    private val factory = BurkanAppComponentFactory()
    private val loader = checkNotNull(javaClass.classLoader)

    @Test
    fun `the plain application of a backup is created as it is`() {
        val application = factory.instantiateApplicationCompat(loader, Application::class.java.name)

        assertEquals(Application::class.java, application.javaClass)
    }

    @Test
    fun `the app's own application is still handed to Metro`() {
        val application = factory.instantiateApplicationCompat(loader, RoborazziTestApplication::class.java.name)

        assertIs<MetroApplication>(application)
    }

    @Test
    fun `the crash screen is created without the graph`() {
        // No application has been handed to the factory, so there is no graph to ask.
        val activity = factory.instantiateActivityCompat(loader, CrashActivity::class.java.name, null)

        assertIs<CrashActivity>(activity)
    }
}
