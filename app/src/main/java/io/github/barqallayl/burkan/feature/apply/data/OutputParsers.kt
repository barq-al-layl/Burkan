package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.parseComponentPackage

/*
 * Parsers for the shell output formats in docs/device-notes.md. Each takes the raw stdout and returns domain
 * values; raw output goes no further than this package.
 */

/** `pm list packages`: one `package:<name>` per line. Lines that are not a valid package name are skipped. */
fun parsePackageList(stdout: String): List<PackageName> =
    stdout.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith(PACKAGE_PREFIX) }
        .mapNotNull { PackageName.parse(it.removePrefix(PACKAGE_PREFIX)) }
        .distinct()
        .toList()

/** `ime list -s`: one component per line; the package is the part before `/`. */
fun parseInputMethodPackages(stdout: String): Set<PackageName> =
    stdout.lineSequence().mapNotNull(::parseComponentPackage).toSet()

/**
 * `dumpsys wallpaper`: the first line containing `mWallpaperComponent=`. The first `ComponentInfo{` in the dump is
 * `mDefaultWallpaperComponent`, which is always SystemUI's image wallpaper, so it must not be used.
 */
fun parseWallpaperPackage(stdout: String): PackageName? =
    stdout.lineSequence()
        .firstOrNull { WALLPAPER_MARKER in it }
        ?.substringAfter(WALLPAPER_MARKER)
        ?.let { between(it, COMPONENT_START, "/") }
        ?.let(PackageName::parse)

/**
 * `dumpsys activity processes`: the [candidates] that appear in the dump as a whole token. Splitting on everything
 * that cannot be in a package name keeps `com.android.settings` from matching inside
 * `com.android.settings.intelligence`, and reads the dump once rather than once per package.
 */
fun parseRunningPackages(stdout: String, candidates: Set<PackageName>): Set<PackageName> {
    val byName = candidates.associateBy { it.value }
    return stdout.splitToSequence(NOT_IN_PACKAGE_NAME).mapNotNull { byName[it] }.toSet()
}

/** The packages behind the widgets in use: their providers, and the apps hosting them. */
data class WidgetPackages(val providers: Set<PackageName>, val hosts: Set<PackageName>)

/**
 * `dumpsys appwidget`: top-level sections start in column 0. Providers in use are the `provider=` lines of
 * `Widgets:`; hosts are the `hostId=HostId` lines of `Hosts:`.
 */
fun parseWidgetPackages(stdout: String): WidgetPackages {
    val providers = mutableSetOf<PackageName>()
    val hosts = mutableSetOf<PackageName>()
    var section = ""
    stdout.lineSequence().forEach { line ->
        if (line.isNotEmpty() && !line.first().isWhitespace()) {
            section = line.trimEnd()
        } else if (section == "Widgets:" && "provider=" in line) {
            between(line, COMPONENT_START, "/")?.let(PackageName::parse)?.let(providers::add)
        } else if (section == "Hosts:" && "hostId=HostId" in line) {
            between(line, "pkg:", "}")?.let(PackageName::parse)?.let(hosts::add)
        }
    }
    return WidgetPackages(providers = providers, hosts = hosts)
}

private fun between(text: String, start: String, end: String): String? {
    val from = text.indexOf(start).takeIf { it >= 0 }?.plus(start.length) ?: return null
    val to = text.indexOf(end, startIndex = from).takeIf { it >= 0 } ?: return null
    return text.substring(from, to)
}

private const val PACKAGE_PREFIX = "package:"
private const val WALLPAPER_MARKER = "mWallpaperComponent="
private const val COMPONENT_START = "ComponentInfo{"
private val NOT_IN_PACKAGE_NAME = Regex("[^A-Za-z0-9_.]+")
