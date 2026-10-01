package io.github.barqallayl.burkan.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import kotlin.test.assertEquals

class RouteSerializationTest {

    private val routes: List<Route> = listOf(SetupRoute, HomeRoute, SettingsRoute, LogRoute)

    @Test
    fun `every route is listed here`() {
        // The sealed serializer knows every subtype, so a route added without a line above fails this test.
        val declared = Route.serializer().descriptor.getElementDescriptor(1).elementNames.toSet()
        val listed = routes.map { route ->
            Json.encodeToJsonElement(Route.serializer(), route).jsonObject.getValue("type").jsonPrimitive.content
        }.toSet()

        assertEquals(declared, listed)
    }

    @Test
    fun `every route round-trips`() {
        routes.forEach { route ->
            val encoded = Json.encodeToString(Route.serializer(), route)

            assertEquals(route, Json.decodeFromString(Route.serializer(), encoded))
        }
    }

    @Test
    fun `the back stack round-trips with the serializer the navigator saves it with`() {
        val serializer = NavBackStackSerializer(Route.serializer())
        val backStack = NavBackStack(*routes.toTypedArray())

        val decoded = Json.decodeFromString(serializer, Json.encodeToString(serializer, backStack))

        assertEquals(routes, decoded.toList())
    }
}
