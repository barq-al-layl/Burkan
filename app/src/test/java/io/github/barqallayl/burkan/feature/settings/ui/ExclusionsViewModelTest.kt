package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.ui.graphics.ImageBitmap
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.awaitStateMatching
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.apply.data.FullApplyPlan
import io.github.barqallayl.burkan.feature.settings.data.AppIcons
import io.github.barqallayl.burkan.feature.settings.data.InstalledApps
import io.github.barqallayl.burkan.feature.settings.data.InstalledAppsStore
import io.github.barqallayl.burkan.feature.settings.model.AppList
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import io.github.barqallayl.burkan.feature.settings.model.matching
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals

class ExclusionsViewModelTest {

    private val chat = PackageName.known("org.example.chat")
    private val maps = PackageName.known("com.example.maps")
    private val gone = PackageName.known("com.example.gone")
    private val installedFixed = FullApplyPlan.FixedExclusions.first()

    private val settings = FakeSettingsStorage()
    private val installedApps = FakeInstalledApps()
    private val icons = object : AppIcons {
        override fun cached(packageName: PackageName): ImageBitmap? = null

        override suspend fun icon(packageName: PackageName): ImageBitmap? = null
    }

    private fun TestScope.viewModel() = ExclusionsViewModel(
        settings,
        SettingsStore(settings, FakeDeviceStateStorage(), backgroundScope),
        InstalledAppsStore(installedApps, settings, backgroundScope),
        icons,
    )

    @Test
    fun `apps are listed by name, with an excluded app since uninstalled kept so it can be removed`() = runTest {
        settings.userExclusions.value = setOf(gone)

        viewModel().testWithInternalState(this) {
            val reading = runOnCreate()
            val listed = awaitStateMatching { it.apps is AppList.Loaded && it.excluded.isNotEmpty() }

            assertEquals(
                AppList.Loaded(
                    listOf(ExcludableApp(chat, "Chat"), ExcludableApp(gone, label = null), ExcludableApp(maps, "maps")),
                ),
                listed.apps,
            )
            assertEquals(setOf(gone), listed.excluded)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the fixed exclusions are labelled where installed`() = runTest {
        viewModel().testWithInternalState(this) {
            val reading = runOnCreate()
            val fixed = awaitStateMatching { it.fixed != null }.fixed!!

            assertEquals(ExcludableApp(installedFixed, "Installed"), fixed.first())
            assertEquals(null, fixed.last().label)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `apps that cannot be listed show why, and can be listed again`() = runTest {
        installedApps.result = SettingsError.AppsNotListed.left()

        viewModel().testWithInternalState(this) {
            val reading = runOnCreate()
            assertEquals(
                AppList.Failed(SettingsError.AppsNotListed),
                awaitStateMatching { it.apps is AppList.Failed }.apps,
            )

            installedApps.result = listOf(ExcludableApp(maps, "Maps")).right()
            containerHost.list()

            assertEquals(
                AppList.Loaded(listOf(ExcludableApp(maps, "Maps"))),
                awaitStateMatching { it.apps is AppList.Loaded }.apps,
            )
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `ticking an app excludes it, and ticking it again brings it back`() = runTest {
        viewModel().testWithInternalState(this) {
            val reading = runOnCreate()

            containerHost.toggle(chat).join()
            assertEquals(setOf(chat), awaitStateMatching { chat in it.excluded }.excluded)

            containerHost.toggle(chat).join()
            assertEquals(emptySet(), awaitStateMatching { it.excluded.isEmpty() }.excluded)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a search finds apps by name or by package name, whatever the case`() {
        val apps = listOf(ExcludableApp(chat, "Chat"), ExcludableApp(maps, "Maps"), ExcludableApp(gone, label = null))

        assertEquals(apps, apps.matching("  "))
        assertEquals(listOf(ExcludableApp(maps, "Maps")), apps.matching("mAp"))
        assertEquals(listOf(ExcludableApp(chat, "Chat")), apps.matching("org.example"))
        assertEquals(listOf(ExcludableApp(gone, label = null)), apps.matching(" gone "))
        assertEquals(emptyList(), apps.matching("zebra"))
    }

    @Test
    fun `a list already at hand stays on screen while it is listed again`() = runTest {
        val store = InstalledAppsStore(installedApps, settings, backgroundScope)
        store.refresh().join()
        val listed = store.apps.value

        installedApps.result = listOf(ExcludableApp(maps, "Maps")).right()
        val refreshing = store.refresh()

        assertEquals(listed, store.apps.value)
        refreshing.join()
        assertEquals(AppList.Loaded(listOf(ExcludableApp(maps, "Maps"))), store.apps.value)
    }

    private inner class FakeInstalledApps : InstalledApps {
        var result: Either<SettingsError, List<ExcludableApp>> =
            listOf(ExcludableApp(maps, "maps"), ExcludableApp(chat, "Chat")).right()

        override suspend fun launchable() = result

        override suspend fun describe(packages: List<PackageName>): List<ExcludableApp> =
            packages.map { ExcludableApp(it, if (it == installedFixed) "Installed" else null) }
    }
}
