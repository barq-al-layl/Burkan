package io.github.barqallayl.burkan.core.model

/**
 * Which renderer is in use. [newApps] is what the property gives processes started from now on; the others are what
 * the three system surfaces are actually running with.
 */
data class RendererStatus(
    val newApps: Renderer,
    val systemUi: Renderer,
    val launcher: Renderer,
    val keyboard: Renderer,
)
