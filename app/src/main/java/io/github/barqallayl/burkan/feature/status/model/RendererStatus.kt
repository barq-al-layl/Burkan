package io.github.barqallayl.burkan.feature.status.model

import io.github.barqallayl.burkan.core.model.Renderer

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

enum class Headline {
    VulkanActive,

    /** The property is not set, and nothing runs on Vulkan: the usual state after a restart. */
    NotApplied,

    /** Some of it is on Vulkan and some is not. */
    PartlyApplied,

    /** The phone could not be asked. */
    Unknown,
}

/**
 * A surface that cannot be read (not running, say) neither confirms nor contradicts the property, so only surfaces
 * that report a renderer count.
 */
fun RendererStatus.headline(): Headline {
    val surfaces = listOf(systemUi, launcher, keyboard).filter { it != Renderer.Unknown }
    return when {
        newApps == Renderer.Vulkan && surfaces.all { it == Renderer.Vulkan } -> Headline.VulkanActive
        newApps != Renderer.Vulkan && surfaces.none { it == Renderer.Vulkan } -> Headline.NotApplied
        else -> Headline.PartlyApplied
    }
}

/** `getprop debug.hwui.renderer`: `skiavk` is Vulkan; unset or `skiagl` is the default, OpenGL. */
fun rendererFromProperty(value: String): Renderer = when (value.trim()) {
    "skiavk" -> Renderer.Vulkan
    "", "skiagl" -> Renderer.OpenGL
    else -> Renderer.Unknown
}
