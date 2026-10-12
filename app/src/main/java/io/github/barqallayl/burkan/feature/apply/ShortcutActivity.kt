package io.github.barqallayl.burkan.feature.apply

import android.app.Activity
import android.os.Bundle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import io.github.barqallayl.burkan.feature.apply.data.ExternalTrigger
import kotlinx.coroutines.runBlocking

/**
 * What the app's shortcuts open: it starts the run the shortcut names and closes at once, with nothing to show.
 * The shortcuts are on the app's icon, and they are what an automation that lists an app's shortcuts, as Samsung's
 * Modes and Routines is expected to, offers as the app's actions.
 *
 * Not exported: the launcher starts a shortcut in the app's own name, so no other app can start this.
 */
@Inject
@ActivityKey
@ContributesIntoMap(AppScope::class, binding = binding<Activity>())
class ShortcutActivity(private val trigger: ExternalTrigger) : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One small read of the phone's state file; the activity has to finish before it is ever shown, so it
        // cannot wait for it anywhere else.
        runBlocking { trigger.onShortcut(intent?.action) }
        finish()
    }
}
