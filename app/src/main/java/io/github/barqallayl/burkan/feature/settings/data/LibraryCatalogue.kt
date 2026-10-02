package io.github.barqallayl.burkan.feature.settings.data

import android.app.Application
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import com.mikepenz.aboutlibraries.Libs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The libraries the app ships, with their licences. */
interface LibraryCatalogue {
    suspend fun read(): Either<SettingsError, Libs>
}

/** Reads what the AboutLibraries Gradle plugin generated at build time into `R.raw.aboutlibraries`. */
@Inject
@ContributesBinding(AppScope::class)
class GeneratedLibraryCatalogue(private val application: Application) : LibraryCatalogue {

    override suspend fun read(): Either<SettingsError, Libs> = withContext(Dispatchers.IO) {
        try {
            val json = application.resources.openRawResource(R.raw.aboutlibraries)
                .bufferedReader()
                .use { it.readText() }
            Libs.Builder().withJson(json).build().right()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A missing or malformed resource is a build fault; the screen says so rather than crashing.
            Logger.e(e) { "Library list unreadable" }
            SettingsError.LicencesUnreadable.left()
        }
    }
}
