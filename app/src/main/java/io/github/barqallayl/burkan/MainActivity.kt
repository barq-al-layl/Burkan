package io.github.barqallayl.burkan

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import io.github.barqallayl.burkan.core.store.StoreWarmUp

@Inject
@ActivityKey
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
class MainActivity(
    private val viewModelFactory: MetroViewModelFactory,
    private val stores: StoreWarmUp,
) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        stores.start()
        setContent {
            App(viewModelFactory, onThemeChange = ::styleSystemBars)
        }
    }

    /**
     * Colours the status and navigation bar icons for the app's theme, which need not be the system's: left alone,
     * a dark app on a light phone keeps dark icons on a dark bar.
     */
    private fun styleSystemBars(isDark: Boolean) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        window.isNavigationBarContrastEnforced = false
    }
}
