package io.github.barqallayl.burkan.core.model

/** How the app gets a shell on the phone. */
enum class ShellBackend {
    /** Pairs with the phone's own wireless debugging and connects to its daemon. The default. */
    WirelessDebugging,

    /** Asks Shizuku, which the user keeps running, for a shell. */
    Shizuku,
}
