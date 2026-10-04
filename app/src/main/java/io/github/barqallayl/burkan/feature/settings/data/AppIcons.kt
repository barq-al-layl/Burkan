package io.github.barqallayl.burkan.feature.settings.data

import android.app.Application
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.shell.PackageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App icons, loaded one at a time as rows come into view: a few hundred at once would not fit in memory. */
interface AppIcons {
    /** The icon of [packageName] if it has been loaded lately, so a row can draw it in its first frame. */
    fun cached(packageName: PackageName): ImageBitmap?

    /** The icon of [packageName], or null when it is not installed. */
    suspend fun icon(packageName: PackageName): ImageBitmap?
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PackageManagerAppIcons(private val application: Application) : AppIcons {

    private val cache = LruCache<PackageName, ImageBitmap>(CACHED_ICONS)

    /** The size a list row draws them at, in pixels. */
    private val size: Int get() = (ICON_DP * application.resources.displayMetrics.density).toInt()

    override fun cached(packageName: PackageName): ImageBitmap? = cache[packageName]

    override suspend fun icon(packageName: PackageName): ImageBitmap? = cache[packageName]
        ?: withContext(Dispatchers.IO) {
            try {
                application.packageManager.getApplicationIcon(packageName.value).toBitmap(size, size).asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }?.also { cache.put(packageName, it) }

    private companion object {
        const val ICON_DP = 40
        const val CACHED_ICONS = 100
    }
}
