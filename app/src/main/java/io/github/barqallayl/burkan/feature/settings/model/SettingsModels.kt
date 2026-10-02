package io.github.barqallayl.burkan.feature.settings.model

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.PackageName

/** An app the user can keep out of the full apply. [label] is null for a package that is not installed. */
data class ExcludableApp(val packageName: PackageName, val label: String?)

sealed interface SettingsError : AppError {
    /** Android would not list the installed apps. */
    data object AppsNotListed : SettingsError

    /** The list of libraries built into the app could not be read. */
    data object LicencesUnreadable : SettingsError
}

object About {
    const val SOURCE_URL = "https://github.com/barq-al-layl/Burkan"
    const val LICENCE_URL = "$SOURCE_URL/blob/main/LICENSE"
}
