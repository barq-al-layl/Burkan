package io.github.barqallayl.burkan.feature.apply

import android.app.Activity
import android.os.Bundle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import io.github.barqallayl.burkan.feature.apply.data.ExternalTrigger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * What the app's shortcuts open: it starts the run the shortcut names and closes at once, with nothing to show.
 * The shortcuts are on the app's icon, and they are what an automation that lists an app's shortcuts, as Samsung's
 * Modes and Routines is expected to, offers as the app's actions.
 *
 * Its window is translucent and empty rather than `Theme.NoDisplay`, which would have to finish before the end of
 * `onCreate`: the run is started once the phone's state file has been read, off the main thread, and the activity
 * finishes then. Until it finishes it is in front, which is what lets it start the service.
 *
 * Not exported: the launcher starts a shortcut in the app's own name, so no other app can start this.
 */
@Inject
@ActivityKey
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
class ShortcutActivity(private val trigger: ExternalTrigger) : Activity() {

    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action
        scope.launch {
            trigger.onShortcut(action)
            finish()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
