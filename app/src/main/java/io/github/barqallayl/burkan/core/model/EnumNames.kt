package io.github.barqallayl.burkan.core.model

import kotlin.enums.enumEntries

/**
 * The entry of [E] called [name], or null when there is none. Names are what the app stores and passes in intents,
 * so one written by another version of the app may name an entry this one does not have.
 */
inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    name?.let { enumEntries<E>().firstOrNull { entry -> entry.name == it } }
