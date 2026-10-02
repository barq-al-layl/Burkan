package io.github.barqallayl.burkan.feature.settings.ui

import arrow.core.left
import arrow.core.right
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import io.github.barqallayl.burkan.awaitStateMatching
import io.github.barqallayl.burkan.feature.settings.data.LibraryCatalogue
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals

class LicencesViewModelTest {

    private val libraries = Libs(
        libraries = listOf(
            Library(
                uniqueId = "org.example:widgets",
                artifactVersion = "1.0.0",
                name = "Widgets",
                description = null,
                website = null,
                developers = emptyList(),
                organization = null,
                scm = null,
            ),
        ),
        licenses = emptySet(),
    )

    @Test
    fun `the libraries are read once the screen opens`() = runTest {
        val catalogue = object : LibraryCatalogue {
            override suspend fun read() = libraries.right()
        }

        LicencesViewModel(catalogue).testWithInternalState(this) {
            val reading = runOnCreate()

            assertEquals(libraries, awaitStateMatching { it.libraries != null }.libraries)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a list that cannot be read shows why`() = runTest {
        val catalogue = object : LibraryCatalogue {
            override suspend fun read() = SettingsError.LicencesUnreadable.left()
        }

        LicencesViewModel(catalogue).testWithInternalState(this) {
            val reading = runOnCreate()

            assertEquals(SettingsError.LicencesUnreadable, awaitStateMatching { it.error != null }.error)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val catalogue = object : LibraryCatalogue {
            override suspend fun read() = libraries.right()
        }

        LicencesViewModel(catalogue).testWithInternalState(this) {
            containerHost.back()
            expectSideEffect(LicencesSideEffect.Back)
        }
    }
}
