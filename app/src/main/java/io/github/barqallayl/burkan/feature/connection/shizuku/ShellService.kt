package io.github.barqallayl.burkan.feature.connection.shizuku

import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * The shell Shizuku runs for the app, as the user Shizuku runs as: `shell` when Shizuku was started over adb, root
 * when it was started with root or is Sui.
 *
 * It lives in a process of its own, started by Shizuku through `app_process`. That is not an app's process: there is no
 * application, no dependency graph and no usable `Context`, so this class takes nothing from the rest of the app, and
 * the rest of the app takes nothing from it but [TIMED_OUT] and [START_FAILED].
 */
@Keep
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(
        command: String,
        timeoutMillis: Long,
        stdout: ParcelFileDescriptor,
        stderr: ParcelFileDescriptor,
    ): Int = try {
        ParcelFileDescriptor.AutoCloseOutputStream(stdout).use { out ->
            ParcelFileDescriptor.AutoCloseOutputStream(stderr).use { err ->
                execute(command, timeoutMillis, out, err)
            }
        }
    } catch (_: Exception) {
        // Thrown out of here, a checked exception would take this process down, and an unchecked one would reach the
        // app as it is. The app gets a code instead. The command may have started, so this is not START_FAILED.
        FAILED
    }

    private fun execute(command: String, timeoutMillis: Long, out: OutputStream, err: OutputStream): Int {
        val process = try {
            ProcessBuilder(SHELL, "-c", command).start()
        } catch (_: Exception) {
            // An IOException, or a SecurityException where the shell may not be run: nothing ran.
            return START_FAILED
        }
        // Nothing is typed to a command: one that waits for input gets the end of it at once.
        process.outputStream.close()
        val pumps = listOf(pump(process.inputStream, out), pump(process.errorStream, err))
        val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        // A command that left something running behind it can hold its output open for good: only so long is waited.
        pumps.forEach { it.join(PUMP_GRACE_MILLIS) }
        return if (finished) process.exitValue() else TIMED_OUT
    }

    private fun pump(from: InputStream, to: OutputStream): Thread = thread(isDaemon = true) {
        try {
            from.copyTo(to)
        } catch (_: IOException) {
            // The reader went away, or the process was killed under the copy; nothing is left to deliver.
        }
    }

    companion object {
        /** The command was still running when its time ran out, and was killed. Exit codes are never negative. */
        const val TIMED_OUT = -1

        /** The command could not be started. */
        const val START_FAILED = -2

        /** Something failed after the command may have started: what it did is not known. */
        const val FAILED = -3

        private const val SHELL = "/system/bin/sh"
        private const val PUMP_GRACE_MILLIS = 2_000L
    }
}
