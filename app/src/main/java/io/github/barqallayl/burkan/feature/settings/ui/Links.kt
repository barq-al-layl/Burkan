package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.ui.platform.UriHandler

/**
 * Opens [url] in whatever the phone has for it. A phone with no browser has nothing, which Compose reports by
 * throwing: the tap then does nothing, and the app goes on.
 */
fun UriHandler.openIfPossible(url: String) {
    try {
        openUri(url)
    } catch (_: IllegalArgumentException) {
        // Nothing on the phone opens links.
    }
}
