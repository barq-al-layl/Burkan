package io.github.barqallayl.burkan.feature.settings.data

import android.app.Application
import com.mikepenz.aboutlibraries.Libs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The libraries the app ships, with their licences. */
interface LibraryCatalogue {
    suspend fun read(): Libs
}

/** Reads what the AboutLibraries Gradle plugin generated at build time into `R.raw.aboutlibraries`. */
@Inject
@ContributesBinding(AppScope::class)
class GeneratedLibraryCatalogue(private val application: Application) : LibraryCatalogue {

    override suspend fun read(): Libs = withContext(Dispatchers.IO) {
        val json = application.resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use { it.readText() }
        Libs.Builder().withJson(json).build()
    }
}
