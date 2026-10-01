package io.github.barqallayl.burkan.feature.settings.ui

import io.github.barqallayl.burkan.awaitStateMatching
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.settings.data.InstalledApps
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals

class ExclusionsViewModelTest {

    private val chat = PackageName.known("org.example.chat")
    private val maps = PackageName.known("com.example.maps")
    private val gone = PackageName.known("com.example.gone")

    private val settings = FakeSettingsStorage()
    private val installedApps = object : InstalledApps {
        override suspend fun launchable() = listOf(ExcludableApp(maps, "maps"), ExcludableApp(chat, "Chat"))
    }

    @Test
    fun `apps are listed by name, with an excluded app since uninstalled kept so it can be removed`() = runTest {
        settings.userExclusions.value = setOf(gone)

        ExclusionsViewModel(settings, installedApps).testWithInternalState(this) {
            val reading = runOnCreate()
            val listed = awaitStateMatching { it.apps != null && it.excluded.isNotEmpty() }

            assertEquals(
                listOf(ExcludableApp(chat, "Chat"), ExcludableApp(gone, label = null), ExcludableApp(maps, "maps")),
                listed.apps,
            )
            assertEquals(setOf(gone), listed.excluded)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `ticking an app excludes it, and ticking it again brings it back`() = runTest {
        ExclusionsViewModel(settings, installedApps).testWithInternalState(this) {
            val reading = runOnCreate()

            containerHost.toggle(chat).join()
            assertEquals(setOf(chat), awaitStateMatching { chat in it.excluded }.excluded)

            containerHost.toggle(chat).join()
            assertEquals(emptySet(), awaitStateMatching { it.excluded.isEmpty() }.excluded)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }
}
