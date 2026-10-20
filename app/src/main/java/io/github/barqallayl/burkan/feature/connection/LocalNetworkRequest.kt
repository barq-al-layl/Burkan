package io.github.barqallayl.burkan.feature.connection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.feature.connection.data.LOCAL_NETWORK_PERMISSION

/**
 * Asks for the permission to look for services on the network, which Android 17 needs before wireless debugging
 * can be found. Android stops showing its dialog once the user has refused twice, and then refuses at once; that
 * refusal opens the app's page in Settings instead, where the permission is under Nearby devices.
 */
@Composable
fun rememberLocalNetworkRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val activity = LocalActivity.current
    // Whether Android would explain itself before this request: false both before and after a refusal means its
    // dialog never showed.
    val explainedBefore = remember { booleanArrayOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && activity != null && !explainedBefore[0] && !activity.explains()) {
            activity.openSettings(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
            )
        }
        onResult(granted)
    }
    return {
        explainedBefore[0] = activity?.explains() == true
        launcher.launch(LOCAL_NETWORK_PERMISSION)
    }
}

private fun Activity.explains(): Boolean = shouldShowRequestPermissionRationale(LOCAL_NETWORK_PERMISSION)
