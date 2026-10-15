package io.github.barqallayl.burkan.feature.status.model

import io.github.barqallayl.burkan.core.shell.PackageName
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
 * A surface that cannot be read (not running, say), or has drawn nothing yet, neither confirms nor contradicts the
 * property, so only surfaces that report a renderer count.
 */
fun RendererStatus.headline(): Headline {
    val surfaces = listOf(systemUi, launcher, keyboard).filter { it == Renderer.Vulkan || it == Renderer.OpenGL }
    return when {
        newApps == Renderer.Vulkan && surfaces.all { it == Renderer.Vulkan } -> Headline.VulkanActive
        newApps != Renderer.Vulkan && surfaces.none { it == Renderer.Vulkan } -> Headline.NotApplied
        else -> Headline.PartlyApplied
    }
}

/** An open app still drawing with the renderer it started on, after the property moved on. */
data class BehindApp(val packageName: PackageName, val label: String)
