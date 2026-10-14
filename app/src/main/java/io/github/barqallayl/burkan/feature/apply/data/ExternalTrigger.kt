package io.github.barqallayl.burkan.feature.apply.data

import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import kotlinx.coroutines.flow.first

/**
 * Starts a run for something outside the app's own screens: an automation app such as Tasker, through a broadcast,
 * or one of the app's own shortcuts.
 *
 * Any app on the phone can send the broadcast, and it is acted on without asking: that is the owner's decision,
 * made so that automation works with nothing to switch on. What limits the harm is what a run from here can do.
 * It is one of three fixed runs, it takes no arguments, and it never locks a phone that is in use.
 *
 * Going back to OpenGL is not among the actions. A run from here leaves System UI for the next lock when the phone
 * is in use, and what runs at the lock is an apply of Vulkan.
 */
@Inject
class ExternalTrigger(
    private val deviceState: DeviceStateStorage,
    private val launcher: ApplyLauncher,
) {
    /** A broadcast with [action] arrived from another app. True when a run was started. */
    suspend fun onBroadcast(action: String?): Boolean {
        val kind = kindFor(action) ?: return false
        return start(kind)
    }

    /**
     * One of the app's own shortcuts was used. [action] is the shortcut's: the same three runs as a broadcast
     * offers, under names of their own. True when a run was started.
     */
    suspend fun onShortcut(action: String?): Boolean {
        val kind = when (action) {
            SHORTCUT_APPLY -> ApplyKind.Light
            SHORTCUT_RESTART_ALL -> ApplyKind.Full
            else -> return false
        }
        return start(kind)
    }

    /** The Quick Settings tile was tapped: an apply. */
    suspend fun onTile(): Boolean = start(ApplyKind.Light)

    private suspend fun start(kind: ApplyKind): Boolean {
        if (!deviceState.isSetupComplete.first()) return false
        return launcher.startUnattended(kind, RunTrigger.External)
    }

    companion object {
        const val ACTION_APPLY = "io.github.barqallayl.burkan.action.APPLY"
        const val ACTION_RESTART_OPEN_APPS = "io.github.barqallayl.burkan.action.RESTART_OPEN_APPS"
        const val ACTION_RESTART_ALL = "io.github.barqallayl.burkan.action.RESTART_ALL"

        /** The actions of the shortcuts in `res/xml/shortcuts.xml`. */
        const val SHORTCUT_APPLY = "io.github.barqallayl.burkan.shortcut.APPLY"
        const val SHORTCUT_RESTART_ALL = "io.github.barqallayl.burkan.shortcut.RESTART_ALL"

        fun kindFor(action: String?): ApplyKind? = when (action) {
            ACTION_APPLY -> ApplyKind.Light
            ACTION_RESTART_OPEN_APPS -> ApplyKind.OpenApps
            ACTION_RESTART_ALL -> ApplyKind.Full
            else -> null
        }
    }
}
