package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.right
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class BackendShellAccessTest {

    private val adb = FakeAdbClient()
    private val shizuku = FakeShizukuClient()
    private val settings = FakeSettingsStorage()
    private val discovery = FakeAdbDiscovery().apply {
        endpoints[AdbService.Connect] = AdbEndpoint("192.168.1.20", 37_215)
    }

    private fun TestScope.access() = BackendShellAccess(
        settings,
        AdbShellAccess(
            adb,
            discovery,
            FakeWirelessDebugging(on = true),
            FakeDeviceStateStorage(paired = true),
            settings,
            backgroundScope,
        ),
        ShizukuShellAccess(shizuku, backgroundScope),
    )

    @Test
    fun `until the user chooses otherwise the shell comes from wireless debugging`() = runTest {
        assertEquals("ran".right(), access().withShell { "ran" })

        assertEquals(listOf("connect 127.0.0.1:37215"), adb.calls)
        assertEquals(emptyList(), shizuku.calls)
    }

    @Test
    fun `with Shizuku chosen the shell comes from Shizuku, and wireless debugging is left alone`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku

        assertEquals("ran".right(), access().withShell { "ran" })

        assertEquals(listOf("connect"), shizuku.calls)
        assertEquals(emptyList(), adb.calls)
    }

    @Test
    fun `a choice made between two runs takes effect at the next one`() = runTest {
        val access = access()

        access.withShell { }
        access.release()
        settings.shellBackend.value = ShellBackend.Shizuku
        access.withShell { }

        assertEquals(listOf("connect 127.0.0.1:37215", "disconnect"), adb.calls)
        assertEquals(listOf("connect"), shizuku.calls)
    }

    @Test
    fun `release lets go of both, so a shell kept from before the choice changed is closed`() = runTest {
        val access = access()
        access.withShell { }
        settings.shellBackend.value = ShellBackend.Shizuku
        access.withShell { }

        access.release()

        assertEquals("disconnect", adb.calls.last())
        assertEquals("disconnect", shizuku.calls.last())
    }
}
