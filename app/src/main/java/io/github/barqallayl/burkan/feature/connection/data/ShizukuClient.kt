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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/** The package of Shizuku's own app, which most forks keep. */
private val SHIZUKU_PACKAGE: PackageName = PackageName.known("moe.shizuku.privileged.api")

/** The permission Shizuku's app defines and every app using Shizuku asks for, under whatever name a fork gives it. */
private const val SHIZUKU_API_PERMISSION = "moe.shizuku.manager.permission.API_V23"

/**
 * Shizuku's own app, whatever package a fork gives it: the one that defines [SHIZUKU_API_PERMISSION], or else
 * [SHIZUKU_PACKAGE] when that is installed. Null when there is no app, as with Sui. Asks the package manager.
 */
fun shizukuAppPackage(packageManager: PackageManager): PackageName? =
    definerOf(packageManager, SHIZUKU_API_PERMISSION) ?: SHIZUKU_PACKAGE.takeIf { isInstalled(packageManager, it) }

private fun definerOf(packageManager: PackageManager, permission: String): PackageName? = try {
    PackageName.parse(packageManager.getPermissionInfo(permission, 0).packageName)
} catch (_: PackageManager.NameNotFoundException) {
    null
}

private fun isInstalled(packageManager: PackageManager, name: PackageName): Boolean = try {
    packageManager.getPackageInfo(name.value, PackageManager.PackageInfoFlags.of(0))
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

/**
 * What Burkan needs of Shizuku: whether it is there, may be used, and the shell it hands over, which runs commands as
 * the user Shizuku runs as: `shell` when Shizuku was started over adb, root when it was started with root or is Sui.
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

    /**
     * Shizuku's own app, which a run never force-stops: a stopped app gets no `BOOT_COMPLETED`, so Shizuku would not
     * start itself after the next restart. Null when there is none, as with Sui.
     */
    suspend fun appPackage(): PackageName?

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

    /** Held while a shell is started, so that two callers at once do not wait on one answer. */
    private val connecting = Mutex()

    /** How many shells have been asked for: each gets a tag of its own. */
    private val binds = AtomicInteger()

    /** What the shell now up, or being started, was asked for with: ending it takes the same. */
    @Volatile
    private var bound: Shizuku.UserServiceArgs? = null

    // Asked once, when first needed: nothing needs it while Shizuku is not in use.
    private val version: Int by lazy { versionCode() }

    /**
     * A new tag every time. Shizuku's client keeps what it was told to answer under the tag, and empties it when the
     * shell under that tag dies. A shell started again under the same tag, before the one just ended has died, would
     * be answered to nobody: its start would be waited for until the time ran out.
     */
    private fun newArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(application.packageName, ShellService::class.java.name))
            // Gone with the app's process, as well as with the connection.
            .daemon(false)
            .processNameSuffix(PROCESS_SUFFIX)
            .tag("$TAG-${binds.incrementAndGet()}")
            // A new version of the app has Shizuku start the new service, not keep the old one.
            .version(version)

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

    override suspend fun appPackage(): PackageName? =
        withContext(Dispatchers.IO) { shizukuAppPackage(application.packageManager) }

    override suspend fun connect(): Either<ConnectionError, Unit> = connecting.withLock {
        disconnect()
        if (!Shizuku.pingBinder()) return@withLock ConnectionError.ShizukuNotRunning.left()
        val bound = withTimeoutOrNull(BIND_TIMEOUT) { bind() }
        if (bound == null) return@withLock ConnectionError.ShizukuShellFailed.left()
        // 2000 is `shell`, 0 is root. Root runs the same fixed commands, but only `shell` has been tried on a phone.
        val uid = try {
            Shizuku.getUid().toString()
        } catch (_: RuntimeException) {
            "unknown"
        }
        Logger.i { "Burkan's shell in Shizuku is up, as uid $uid" }
        Unit.right()
    }

    private suspend fun bind(): IShellService? = suspendCancellableCoroutine { continuation ->
        waiting = continuation
        continuation.invokeOnCancellation {
            waiting = null
            unbind()
        }
        try {
            Shizuku.bindUserService(newArgs().also { bound = it }, connection)
        } catch (e: RuntimeException) {
            Logger.w(e) { "Binding Burkan's shell in Shizuku failed" }
            waiting = null
            if (continuation.isActive) continuation.resume(null)
        }
    }

    override suspend fun disconnect() {
        // Asked of what was started, not of what still answers: a shell given up on after a failed command is
        // still running, and is ended here.
        if (bound == null) return
        service = null
        withContext(Dispatchers.IO) { unbind() }
    }

    private fun unbind() {
        val args = bound ?: return
        bound = null
        try {
            // Removing it, as well as letting go of it, has Shizuku end the shell's process.
            Shizuku.unbindUserService(args, connection, true)
        } catch (e: RuntimeException) {
            Logger.w(e) { "Ending Burkan's shell in Shizuku failed" }
        }
    }

    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> = withContext(Dispatchers.IO) {
        val shell = service ?: return@withContext ShellError.NotConnected.left()
        val (stdout, stderr) = try {
            pipes()
        } catch (e: IOException) {
            // The app is out of file descriptors: nothing was sent, and a new shell would not help.
            Logger.w(e) { "No pipe for a command's output" }
            return@withContext ShellError.Failed.left()
        }
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
                    // The shell is still there: losing it is a RemoteException. Reconnecting would not help.
                    ShellService.START_FAILED, ShellService.FAILED -> ShellError.Failed.left()
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
            // Nor is the shell with the next one: it is started afresh.
            service = null
            ShellError.ConnectionLost.left()
        } catch (e: CancellationException) {
            // An IllegalStateException too, so it would be caught below.
            throw e
        } catch (e: RuntimeException) {
            // Binder hands some of the shell's own exceptions back as they are, a SecurityException for one, rather
            // than as a RemoteException. The command may have run, and the shell is not trusted with the next one.
            Logger.w(e) { "Burkan's shell in Shizuku failed a command" }
            service = null
            ShellError.ConnectionLost.left()
        }
    }

    private fun ParcelFileDescriptor.readText(): String =
        ParcelFileDescriptor.AutoCloseInputStream(this).use { it.readBytes().toString(Charsets.UTF_8) }

    /** One pipe for a command's output and one for its errors. The first is closed if the second cannot be made. */
    private fun pipes(): Pair<Array<ParcelFileDescriptor>, Array<ParcelFileDescriptor>> {
        val stdout = ParcelFileDescriptor.createPipe()
        val stderr = try {
            ParcelFileDescriptor.createPipe()
        } catch (e: IOException) {
            stdout.forEach { it.close() }
            throw e
        }
        return stdout to stderr
    }

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
