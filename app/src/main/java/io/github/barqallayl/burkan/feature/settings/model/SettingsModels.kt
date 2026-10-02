package io.github.barqallayl.burkan.feature.settings.model

import io.github.barqallayl.burkan.core.shell.PackageName

/** An app the user can keep out of the full apply. [label] is null for an excluded app no longer installed. */
data class ExcludableApp(val packageName: PackageName, val label: String?)

/** A library the app ships, for the licences list. Names are proper names, shown as they are. */
data class OpenSourceLibrary(val name: String, val licence: String, val url: String)

object About {
    const val SOURCE_URL = "https://github.com/barq-al-layl/Burkan"
    const val LICENCE_URL = "$SOURCE_URL/blob/main/LICENSE"

    private const val APACHE_2 = "Apache License 2.0"
    private const val MIT = "MIT License"

    val libraries: List<OpenSourceLibrary> = listOf(
        OpenSourceLibrary("AndroidX and Jetpack Compose", APACHE_2, "https://developer.android.com/jetpack/androidx"),
        OpenSourceLibrary("Arrow", APACHE_2, "https://github.com/arrow-kt/arrow"),
        OpenSourceLibrary("Bouncy Castle", "Bouncy Castle Licence", "https://www.bouncycastle.org/licence.html"),
        OpenSourceLibrary("Compose Icons (Tabler)", MIT, "https://github.com/composablehorizons/composeicons"),
        OpenSourceLibrary("Conscrypt", APACHE_2, "https://github.com/google/conscrypt"),
        OpenSourceLibrary("Kermit", APACHE_2, "https://github.com/touchlab/Kermit"),
        OpenSourceLibrary("Kotlin and kotlinx", APACHE_2, "https://github.com/JetBrains/kotlin"),
        OpenSourceLibrary("KSafe", APACHE_2, "https://github.com/ioannisa/ksafe"),
        OpenSourceLibrary(
            "libadb-android",
            "Apache License 2.0 or GPL 3.0 or later",
            "https://github.com/MuntashirAkon/libadb-android",
        ),
        OpenSourceLibrary("MaterialKolor", MIT, "https://github.com/jordond/materialkolor"),
        OpenSourceLibrary("Metro", APACHE_2, "https://github.com/ZacSweers/metro"),
        OpenSourceLibrary("Orbit MVI", APACHE_2, "https://github.com/orbit-mvi/orbit-mvi"),
    )
}
