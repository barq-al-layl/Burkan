package io.github.barqallayl.burkan.core.shell

import io.github.barqallayl.burkan.core.model.Renderer

/*
 * Parsers for output that more than one feature reads. The formats are in docs/device-notes.md.
 */

/** `dumpsys gfxinfo <package>`: the first `Pipeline=` line says what the running process renders with. */
fun parseRenderer(stdout: String): Renderer =
    when (stdout.lineSequence().firstOrNull { PIPELINE_MARKER in it }?.substringAfter(PIPELINE_MARKER)?.trim()) {
        "Skia (Vulkan)" -> Renderer.Vulkan
        "Skia (OpenGL)" -> Renderer.OpenGL
        else -> Renderer.Unknown
    }

/** `getprop debug.hwui.renderer`: `skiavk` is Vulkan; unset or `skiagl` is the default, OpenGL. */
fun rendererFromProperty(value: String): Renderer = when (value.trim()) {
    "skiavk" -> Renderer.Vulkan
    "", "skiagl" -> Renderer.OpenGL
    else -> Renderer.Unknown
}

/** `settings get`: the value, or null when the key is unset. `settings get` prints `null` for an unset key. */
fun parseSettingValue(stdout: String): String? =
    stdout.removeSuffix("\n").removeSuffix("\r").takeUnless { it.isEmpty() || it == "null" }

/** The package of a `package/class` component, as `ime list -s` and `default_input_method` give it. */
fun parseComponentPackage(component: String): PackageName? =
    component.trim().takeIf { '/' in it }?.substringBefore('/')?.let(PackageName::parse)

/**
 * `cmd package resolve-activity --brief`: the last line is the `package/class` that answers. When no home app is
 * chosen it is the system's chooser, in the `android` package, which is no home app.
 */
fun parseHomeActivity(stdout: String): PackageName? =
    stdout.lineSequence()
        .lastOrNull { '/' in it }
        ?.let(::parseComponentPackage)
        ?.takeIf { it.value != CHOOSER_PACKAGE }

/** `cmd role get-role-holders`: the holders, separated by `;` or by lines. The first that is a package name. */
fun parseRoleHolders(stdout: String): PackageName? =
    stdout.split(';', '\n').firstNotNullOfOrNull { PackageName.parse(it.trim()) }

private const val PIPELINE_MARKER = "Pipeline="
private const val CHOOSER_PACKAGE = "android"
