package io.github.barqallayl.burkan.feature.connection

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import io.github.barqallayl.burkan.core.ui.rememberPermissionRequest
import io.github.barqallayl.burkan.feature.connection.data.LOCAL_NETWORK_PERMISSION

/**
 * Asks for the permission to look for services on the network, which Android 17 needs before wireless debugging
 * can be found. Once Android stops showing its dialog, the app's page in Settings opens instead, where the permission
 * is under Nearby devices.
 */
@Composable
fun rememberLocalNetworkRequest(onResult: (granted: Boolean) -> Unit): () -> Unit = rememberPermissionRequest(
    permission = LOCAL_NETWORK_PERMISSION,
    settingsIntent = { context ->
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    },
    onResult = onResult,
)
