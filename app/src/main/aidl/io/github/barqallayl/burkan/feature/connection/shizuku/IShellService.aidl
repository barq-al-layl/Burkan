package io.github.barqallayl.burkan.feature.connection.shizuku;

import android.os.ParcelFileDescriptor;

/**
 * The shell Burkan asks Shizuku to run for it: a service in a process of its own, with the shell user's identity.
 *
 * Every method has an id of its own, as Shizuku requires of a user service. 16777114 is the one it reserves for
 * `destroy`.
 */
interface IShellService {
    /** Called by Shizuku when the service is removed: the process ends. */
    void destroy() = 16777114;

    /**
     * Runs [command] as `sh -c` and copies what it prints into the two pipes, which are closed once it ends. Returns
     * its exit code, or [ShellService.TIMED_OUT] when it was still running after [timeoutMillis] and was killed.
     *
     * The output goes through pipes rather than the return value, as a `dumpsys` can print more than a Binder
     * transaction carries.
     */
    int exec(String command, long timeoutMillis, in ParcelFileDescriptor stdout, in ParcelFileDescriptor stderr) = 1;
}
