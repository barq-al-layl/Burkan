package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.fixture
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutputParsersTest {

    @Test
    fun `package list strips the prefix and keeps the order`() {
        val packages = parsePackageList(fixture("pm-list-packages.txt"))

        assertEquals(20, packages.size)
        assertEquals(names("com.android.systemui", "com.sec.android.app.launcher"), packages.take(2))
        assertEquals(PackageName.known("org.example.chat"), packages.last())
    }

    @Test
    fun `package list skips anything that is not a package line`() {
        val stdout = "package:com.example.a\nError: something\npackage:bad name\n\npackage:org.example.b\n"

        assertEquals(names("com.example.a", "org.example.b"), parsePackageList(stdout))
    }

    @Test
    fun `input methods are the part before the slash`() {
        assertEquals(
            names("com.samsung.android.honeyboard", "com.google.android.tts").toSet(),
            parseInputMethodPackages(fixture("ime-list.txt")),
        )
    }

    @Test
    fun `the wallpaper is the current component, not the default one listed first`() {
        assertEquals(
            PackageName.known("com.samsung.android.wallpaper.live"),
            parseWallpaperPackage(fixture("dumpsys-wallpaper.txt")),
        )
    }

    @Test
    fun `no wallpaper component means no wallpaper package`() {
        assertNull(parseWallpaperPackage("mDefaultWallpaperComponent=ComponentInfo{com.android.systemui/.Image}\n"))
    }

    @Test
    fun `running packages are matched as whole tokens`() {
        val installed = parsePackageList(fixture("pm-list-packages.txt")).toSet()

        val running = parseRunningPackages(fixture("dumpsys-activity-processes.txt"), installed)

        assertEquals(
            names(
                "com.android.systemui",
                "com.sec.android.app.launcher",
                "com.samsung.android.honeyboard",
                "com.android.settings",
                "com.example.notes",
                "org.example.weather",
                "org.example.chat",
                "com.sec.imsservice",
            ).toSet(),
            running,
        )
    }

    @Test
    fun `a package whose name extends a running one is not running`() {
        val candidates = names("com.android.settings", "com.android.settings.intelligence").toSet()

        val running = parseRunningPackages("ProcessRecord{1 2:com.android.settings.intelligence/u0a1}", candidates)

        assertEquals(names("com.android.settings.intelligence").toSet(), running)
    }

    @Test
    fun `widget providers come from Widgets and hosts from Hosts`() {
        val widgets = parseWidgetPackages(fixture("dumpsys-appwidget.txt"))

        // net.example.reader offers a widget in Providers: but none is placed, so it is not in use.
        assertEquals(
            names("com.google.android.googlequicksearchbox", "com.example.notes.widget").toSet(),
            widgets.providers,
        )
        assertEquals(
            names("com.samsung.android.app.dressroom", "com.sec.android.app.launcher").toSet(),
            widgets.hosts,
        )
    }

    private fun names(vararg names: String): List<PackageName> = names.map(PackageName::known)
}
