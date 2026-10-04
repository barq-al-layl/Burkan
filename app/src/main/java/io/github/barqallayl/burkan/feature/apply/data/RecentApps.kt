package io.github.barqallayl.burkan.feature.apply.data

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageStatsManager
import android.os.Process
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.shell.PackageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** Which apps the user has opened lately, as Android records it. */
interface RecentApps {
    /** Whether Android will answer: it does only for an app given usage access. */
    fun isAllowed(): Boolean

    /** The apps opened in the last [WINDOW], most recent first. Empty when not allowed, or nothing was. */
    suspend fun byRecency(): List<PackageName>

    companion object {
        /** An app not opened for this long is not "recent" by any limit. */
        val WINDOW = 30.days
    }
}

@Inject
@ContributesBinding(AppScope::class)
class UsageStatsRecentApps(private val application: Application, private val clock: Clock) : RecentApps {

    override fun isAllowed(): Boolean {
        val appOps = application.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            application.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun byRecency(): List<PackageName> = withContext(Dispatchers.IO) {
        val usage = application.getSystemService(UsageStatsManager::class.java)
        val now = clock.now().toEpochMilliseconds()
        usage.queryAndAggregateUsageStats(now - RecentApps.WINDOW.inWholeMilliseconds, now)
            .values
            .filter { it.lastTimeUsed > 0 }
            .sortedByDescending { it.lastTimeUsed }
            .mapNotNull { PackageName.parse(it.packageName) }
    }
}
