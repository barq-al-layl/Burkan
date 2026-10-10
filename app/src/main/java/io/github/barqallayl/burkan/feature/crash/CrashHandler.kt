package io.github.barqallayl.burkan.feature.crash

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import io.github.barqallayl.burkan.feature.crash.ui.CrashActivity
import java.time.ZoneId
import kotlin.system.exitProcess
import kotlin.time.Clock

/**
 * What happens when the app crashes in front of its user: the error is written to a report, and a screen of its own
 * shows it and offers to share it, in place of Android's "Burkan keeps stopping".
 *
 * That screen runs in a process of its own, [CRASH_PROCESS_SUFFIX], which starts after this one has died and
 * touches nothing the rest of the app is built from. So it opens whatever broke, and a crash of its own goes to
 * Android like any other: this handler is not installed there.
 *
 * A crash with no screen of the app showing is left to Android, which handles it quietly: an app may not open a
 * screen from the background, and nobody is looking. The report is still written.
 */
class CrashHandler private constructor(
    private val application: Application,
    private val next: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler, Application.ActivityLifecycleCallbacks {

    /** How many of the app's screens are showing. Only the main thread changes it. */
    @Volatile
    private var started = 0

    override fun uncaughtException(thread: Thread, error: Throwable) {
        val shown = try {
            Log.e(TAG, "Burkan crashed on thread ${thread.name}", error)
            val text = crashReportText(error, thread.name, application.crashSource(), Clock.System.now(), ZoneId.systemDefault())
            CrashReportFile(application).write(text)
            started > 0 && showReport()
        } catch (_: Throwable) {
            // Nothing here may stand in the way of the crash being handled at all.
            false
        }
        if (!shown) {
            next?.uncaughtException(thread, error)
            return
        }
        Process.killProcess(Process.myPid())
        exitProcess(CRASH_EXIT_STATUS)
    }

    /** Opens the crash screen in place of the app's own, which are about to go with this process. */
    private fun showReport(): Boolean {
        val intent = Intent(application, CrashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        application.startActivity(intent)
        return true
    }

    override fun onActivityStarted(activity: Activity) {
        started++
    }

    override fun onActivityStopped(activity: Activity) {
        started--
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    companion object {
        /** The end of the crash screen's process name, as the manifest gives it. */
        const val CRASH_PROCESS_SUFFIX = ":crash"

        /** Installs the handler, in every process of the app but the crash screen's own. */
        fun install(application: Application) {
            if (Application.getProcessName().endsWith(CRASH_PROCESS_SUFFIX)) return
            val handler = CrashHandler(application, Thread.getDefaultUncaughtExceptionHandler())
            application.registerActivityLifecycleCallbacks(handler)
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }

        private const val TAG = "Burkan"
        private const val CRASH_EXIT_STATUS = 10
    }
}
