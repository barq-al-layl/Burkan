package io.github.barqallayl.burkan.feature.settings.model

import io.github.barqallayl.burkan.core.shell.PackageName

/** An app the user can keep out of the full apply. [label] is null for an excluded app no longer installed. */
data class ExcludableApp(val packageName: PackageName, val label: String?)

object About {
    const val SOURCE_URL = "https://github.com/barq-al-layl/Burkan"
    const val LICENCE_URL = "$SOURCE_URL/blob/main/LICENSE"
}
