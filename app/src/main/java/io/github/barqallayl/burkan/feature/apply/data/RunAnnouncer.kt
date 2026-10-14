package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.service.quicksettings.TileService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.feature.apply.ApplyTileService
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry

/** Tells what is outside the app that a run has ended. */
interface RunAnnouncer {
    fun finished(run: RunLogEntry)
}

/**
 * Says so twice. To the Quick Settings tile, which then reads its state again. And to whichever app is listening,
 * as a broadcast: an automation that started a run, or that only wants to know when one ends, learns how it went.
 *
 * The broadcast carries the run's kind, what started it, its result and, when it failed, why, each as the name
 * the app's own log uses. Nothing in it names an app or says anything about the phone, so it is sent to whoever
 * registered for it. Android delivers such a broadcast only to receivers registered while their app is running,
 * which is how Tasker and the like listen.
 */
@Inject
@ContributesBinding(AppScope::class)
class BroadcastRunAnnouncer(private val application: Application) : RunAnnouncer {

    override fun finished(run: RunLogEntry) {
        TileService.requestListeningState(application, ComponentName(application, ApplyTileService::class.java))
        application.sendBroadcast(
            Intent(ACTION_RUN_FINISHED)
                .putExtra(EXTRA_KIND, run.kind.name)
                .putExtra(EXTRA_TRIGGER, run.trigger.name)
                .putExtra(EXTRA_RESULT, run.result.name)
                .putExtra(EXTRA_ERROR, (run.error ?: run.steps.firstNotNullOfOrNull { it.error })?.name),
        )
    }

    companion object {
        const val ACTION_RUN_FINISHED = "io.github.barqallayl.burkan.action.RUN_FINISHED"

        /** `Light`, `Full`, `OpenApps` or `Revert`. */
        const val EXTRA_KIND = "kind"

        /** `Manual`, `Boot`, `AtLock` or `External`. */
        const val EXTRA_TRIGGER = "trigger"

        /** `Succeeded`, `AlreadyApplied`, `Postponed`, `Failed` or `Cancelled`. */
        const val EXTRA_RESULT = "result"

        /** Why it failed, as the name of the error; absent when it did not fail. */
        const val EXTRA_ERROR = "error"
    }
}
