package io.github.barqallayl.burkan.feature.apply.data

import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Starts a run for something outside the app's own screens: an automation app such as Tasker, through a broadcast,
 * or one of the app's own shortcuts.
 *
 * Any app on the phone can send the broadcast, and it is acted on without asking: that is the owner's decision,
 * made so that automation works with nothing to switch on. What limits the harm is what a run from here can do.
 * It is one of three fixed runs, it takes no arguments, and it never locks a phone that is in use. The user can
 * switch the broadcasts off in Settings, and the same run is not started again by one within [MIN_INTERVAL] of
 * its last end, so no app can keep the phone's apps restarting.
 *
 * Going back to OpenGL is not among the actions. A run from here leaves System UI for the next lock when the phone
 * is in use, and what runs at the lock is an apply of Vulkan.
 */
@Inject
class ExternalTrigger(
    private val deviceState: DeviceStateStorage,
    private val launcher: ApplyLauncher,
    private val alerts: RunAlerts,
    private val settings: SettingsStorage,
    private val runLog: RunLogStorage,
    private val clock: Clock,
) {
    /**
     * A broadcast with [action] arrived from another app. True when a run was started. When Android would not let it
     * start from the background, a notification offers to start it instead of nothing happening. One the user has
     * switched off, or that comes too soon after the same run, is passed over in silence.
     */
    suspend fun onBroadcast(action: String?): Boolean {
        val kind = kindFor(action) ?: return false
        if (!settings.allowExternalTriggers.first()) return false
        if (endedLately(kind)) return false
        return start(kind)
    }

    /** Whether a run of [kind] started from outside the app ended less than [MIN_INTERVAL] ago. */
    private suspend fun endedLately(kind: ApplyKind): Boolean {
        val last = runLog.runs.first().firstOrNull { it.trigger == RunTrigger.External && it.kind == kind }
            ?: return false
        val since = clock.now() - (last.startedAt + last.duration)
        // A run that ended in the future means the clock was set back: it says nothing about the last minute.
        return since >= Duration.ZERO && since < MIN_INTERVAL
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
        val started = launcher.startUnattended(kind, RunTrigger.External)
        if (!started) alerts.showNotStarted(kind)
        return started
    }

    companion object {
        const val ACTION_APPLY = "io.github.barqallayl.burkan.action.APPLY"
        const val ACTION_RESTART_OPEN_APPS = "io.github.barqallayl.burkan.action.RESTART_OPEN_APPS"
        const val ACTION_RESTART_ALL = "io.github.barqallayl.burkan.action.RESTART_ALL"

        /** The actions of the shortcuts in `res/xml/shortcuts.xml`. */
        const val SHORTCUT_APPLY = "io.github.barqallayl.burkan.shortcut.APPLY"
        const val SHORTCUT_RESTART_ALL = "io.github.barqallayl.burkan.shortcut.RESTART_ALL"

        /** How long after a run from outside ends before a broadcast may start the same run again. */
        val MIN_INTERVAL = 1.minutes

        fun kindFor(action: String?): ApplyKind? = when (action) {
            ACTION_APPLY -> ApplyKind.Light
            ACTION_RESTART_OPEN_APPS -> ApplyKind.OpenApps
            ACTION_RESTART_ALL -> ApplyKind.Full
            else -> null
        }
    }
}
