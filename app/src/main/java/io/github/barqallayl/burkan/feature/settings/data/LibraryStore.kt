package io.github.barqallayl.burkan.feature.settings.data

import arrow.core.Either
import com.mikepenz.aboutlibraries.Libs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The libraries the app ships, read once and kept: they cannot change while the app runs. */
@Inject
@SingleIn(AppScope::class)
class LibraryStore(
    private val catalogue: LibraryCatalogue,
    @Named(AppBindings.APP_SCOPE) private val scope: CoroutineScope,
) {
    private val _libraries = MutableStateFlow<Either<SettingsError, Libs>?>(null)

    /** Null until read. */
    val libraries: StateFlow<Either<SettingsError, Libs>?> = _libraries.asStateFlow()

    private var loading: Job? = null

    /** Reads the libraries, unless they have been read or are being read. */
    fun load() {
        if (loading != null) return
        loading = scope.launch { _libraries.value = catalogue.read() }
    }
}
