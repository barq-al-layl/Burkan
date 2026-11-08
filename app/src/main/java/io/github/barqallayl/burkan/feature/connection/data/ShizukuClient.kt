package io.github.barqallayl.burkan.feature.connection.data

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommand
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellResult
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.shizuku.IShellService
import io.github.barqallayl.burkan.feature.connection.shizuku.ShellService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/** The package of Shizuku's own app, which Shizuku's forks keep. A run never force-stops it. */
val SHIZUKU_PACKAGE: PackageName = PackageName.known("moe.shizuku.privileged.api")

/**
 * What Burkan needs of Shizuku: whether it is there, may be used, and the shell it hands over, which runs commands as
 * the user Shizuku runs as (`shell`, when Shizuku was started over adb).
 *
 * Running a command without a connected shell is [ShellError.NotConnected].
 */
interface ShizukuClient : ShellExecutor {
    /** Shizuku is running: its binder is here. */
    fun isRunning(): Boolean

    /** Shizuku is running and the user has let Burkan use it. */
    fun isAllowed(): Boolean

    /** Asks Shizuku to let Burkan use it, which it does with a dialog of its own. [isAllowed] has the answer. */
    fun requestPermission()

    /** Has Shizuku start Burkan's shell, and waits for it. Any shell from before is dropped first. */
    suspend fun connect(): Either<ConnectionError, Unit>

    /** Ends Burkan's shell, which has Shizuku end its process. */
    suspend fun disconnect()
}

/** [ShizukuClient] on Shizuku's API, through a user service: Burkan's [ShellService], in a process of its own. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RikkaShizukuClient(private val application: Application) : ShizukuClient {

    @Volatile
    private var service: IShellService? = null

    /** The wait for the service to come up, while there is one. */
    @Volatile
    private var waiting: CancellableContinuation<IShellService?>? = null

    /** One for good: Shizuku keeps every connection it is given, so one made each time would answer more than once. */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            val bound = binder?.takeIf { it.pingBinder() }?.let { IShellService.Stub.asInterface(it) }
            service = bound
            val wait = waiting
            waiting = null
            if (wait != null && wait.isActive) wait.resume(bound)
        }

        // Said late for a shell that was ended on purpose, and then it would take down the one that replaced it. A
        // shell that died is found out by the next command, which fails with a RemoteException.
        override fun onServiceDisconnected(name: ComponentName) = Unit
    }

    // Built when first used: the version asks the package manager, and nothing needs it while Shizuku is not in use.
    private val args: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(application.packageName, ShellService::class.java.name))
            // Gone with the app's process, as well as with the connection.
            .daemon(false)
            .processNameSuffix(PROCESS_SUFFIX)
            .tag(TAG)
            // A new version of the app has Shizuku start the new service, not keep the old one.
            .version(versionCode())
    }

    override fun isRunning(): Boolean = Shizuku.pingBinder()

    override fun isAllowed(): Boolean = try {
        Shizuku.pingBinder() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: RuntimeException) {
        // The binder went away between the ping and the question.
        false
    }

    override fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(REQUEST_CODE)
        } catch (e: RuntimeException) {
            Logger.w(e) { "Asking Shizuku for its permission failed" }
        }
    }

    override suspend fun connect(): Either<ConnectionError, Unit> {
        disconnect()
        if (!Shizuku.pingBinder()) return ConnectionError.ShizukuNotRunning.left()
        val bound = withTimeoutOrNull(BIND_TIMEOUT) { bind() }
        return if (bound == null) ConnectionError.ShizukuShellFailed.left() else Unit.right()
    }

    private suspend fun bind(): IShellService? = suspendCancellableCoroutine { continuation ->
        waiting = continuation
        continuation.invokeOnCancellation {
            waiting = null
            unbind()
        }
        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: RuntimeException) {
            Logger.w(e) { "Binding Burkan's shell in Shizuku failed" }
            waiting = null
            if (continuation.isActive) continuation.resume(null)
        }
    }

    override suspend fun disconnect() {
        if (service == null && waiting == null) return
        service = null
        withContext(Dispatchers.IO) { unbind() }
    }

    private fun unbind() {
        try {
            // Removing it, as well as letting go of it, has Shizuku end the shell's process.
            Shizuku.unbindUserService(args, connection, true)
        } catch (e: RuntimeException) {
            Logger.w(e) { "Ending Burkan's shell in Shizuku failed" }
        }
    }

    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> = withContext(Dispatchers.IO) {
        val shell = service ?: return@withContext ShellError.NotConnected.left()
        val stdout = ParcelFileDescriptor.createPipe()
        val stderr = ParcelFileDescriptor.createPipe()
        try {
            coroutineScope {
                val output = async { stdout[READ].readText() }
                val errors = async { stderr[READ].readText() }
                val exitCode = try {
                    // The shell ends the command when its time is up, which a blocked Binder call cannot be made to.
                    shell.exec(command.line, command.timeout.inWholeMilliseconds, stdout[WRITE], stderr[WRITE])
                } finally {
                    // The shell has copies of these ends. Until ours are closed as well, the readers never see the end.
                    stdout[WRITE].close()
                    stderr[WRITE].close()
                }
                when (exitCode) {
                    ShellService.TIMED_OUT -> ShellError.TimedOut.left()
                    ShellService.START_FAILED -> ShellError.ConnectionLost.left()
                    else -> ShellResult(exitCode, output.await(), errors.await()).right()
                }
            }
        } catch (e: RemoteException) {
            Logger.w(e) { "Burkan's shell in Shizuku went away" }
            service = null
            ShellError.ConnectionLost.left()
        } catch (e: IOException) {
            // A pipe that failed under a read or a close: the output is not whole, so the command is not trusted.
            Logger.w(e) { "Reading a command's output from Burkan's shell in Shizuku failed" }
            ShellError.ConnectionLost.left()
        }
    }

    private fun ParcelFileDescriptor.readText(): String =
        ParcelFileDescriptor.AutoCloseInputStream(this).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun versionCode(): Int = application.packageManager
        .getPackageInfo(application.packageName, PackageManager.PackageInfoFlags.of(0))
        .longVersionCode.toInt()

    private companion object {
        /** Names the shell's process, after the app's own: `io.github.barqallayl.burkan:shell`. */
        const val PROCESS_SUFFIX = "shell"
        const val TAG = "burkan-shell"
        const val REQUEST_CODE = 1
        const val READ = 0
        const val WRITE = 1
        val BIND_TIMEOUT = 10.seconds
    }
}
