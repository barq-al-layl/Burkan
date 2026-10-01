package io.github.barqallayl.burkan.core.model

/**
 * A failure the app expects and can describe. Each feature adds its own errors as a sealed type implementing this,
 * in the feature's `model/`.
 *
 * Not sealed itself: Kotlin requires a sealed type's direct subtypes to share its package, and feature errors live
 * in their features.
 */
interface AppError
