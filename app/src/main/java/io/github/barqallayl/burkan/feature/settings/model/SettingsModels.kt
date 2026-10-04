package io.github.barqallayl.burkan.feature.settings.model

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.PackageName

/**
 * An app the user can keep out of the full apply. [label] is null for a package that is not installed. [isSystem]
 * says it came with the phone rather than being installed by the user.
 */
data class ExcludableApp(val packageName: PackageName, val label: String?, val isSystem: Boolean = false)

/** Which of the apps the list shows. */
enum class AppFilter {
    All,

    /** The ones ticked: never restarted. */
    Selected,

    /** Installed by the user. */
    User,

    /** Came with the phone. */
    System,
}

/**
 * The apps that pass [filter] and that [query] finds, by name or by package name. Nothing typed finds them all.
 * [selected] are the ticked ones, which [AppFilter.Selected] keeps.
 */
fun List<ExcludableApp>.matching(
    query: String,
    filter: AppFilter = AppFilter.All,
    selected: Set<PackageName> = emptySet(),
): List<ExcludableApp> {
    val words = query.trim()
    return filter { app ->
        val passes = when (filter) {
            AppFilter.All -> true
            AppFilter.Selected -> app.packageName in selected
            AppFilter.User -> !app.isSystem
            AppFilter.System -> app.isSystem
        }
        passes && (
            words.isEmpty() ||
                app.label?.contains(words, ignoreCase = true) == true ||
                app.packageName.value.contains(words, ignoreCase = true)
            )
    }
}

/** The installed apps the user can choose from. */
sealed interface AppList {
    data object Loading : AppList
    data class Loaded(val apps: List<ExcludableApp>) : AppList
    data class Failed(val error: AppError) : AppList
}

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
