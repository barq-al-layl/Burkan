package io.github.barqallayl.burkan.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Apps
import io.github.barqallayl.burkan.core.shell.PackageName

/**
 * How a row gets its app's icon: [cached] at once when it was loaded before, [load] otherwise. Null leaves a
 * placeholder.
 */
class IconLoader(
    val cached: (PackageName) -> ImageBitmap?,
    val load: suspend (PackageName) -> ImageBitmap?,
) {
    companion object {
        /** Loads nothing: every row keeps its placeholder. For previews. */
        val None = IconLoader({ null }, { null })
    }
}

/** The app's own icon once loaded; a generic one until then, and for a package that is not installed. */
@Composable
fun AppIcon(packageName: PackageName, icon: IconLoader) {
    val bitmap by produceState(initialValue = icon.cached(packageName), packageName) {
        value = icon.load(packageName)
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Icon(
            Tabler.Outline.Apps,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp),
        )
    }
}
