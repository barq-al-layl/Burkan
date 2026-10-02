package io.github.barqallayl.burkan.feature.status.model

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus

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
