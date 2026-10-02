package io.github.barqallayl.burkan.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Every destination. Sealed, so the back stack serializes without a polymorphic module. */
@Serializable
sealed interface Route : NavKey

@Serializable
data object SetupRoute : Route

@Serializable
data object HomeRoute : Route

@Serializable
data object SettingsRoute : Route

@Serializable
data object LogRoute : Route

@Serializable
data object ExclusionsRoute : Route

@Serializable
data object LicencesRoute : Route
