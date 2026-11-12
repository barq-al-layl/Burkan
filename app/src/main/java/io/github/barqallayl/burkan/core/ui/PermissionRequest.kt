package io.github.barqallayl.burkan.core.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Asks for [permission], and, when Android no longer shows its dialog, opens the place in Settings where the user
 * can grant it instead. Android stops showing the dialog once the user has refused twice, and then refuses at once;
 * without this, the button that asks would do nothing at all.
 *
 * A refusal is taken as one the user never saw when Android would explain itself neither before the request nor
 * after it. [settingsIntent] is the Settings screen to open then, and [onResult] hears every answer.
 */
@Composable
fun rememberPermissionRequest(
    permission: String,
    settingsIntent: (Context) -> Intent,
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val activity = LocalActivity.current
    val currentOnResult by rememberUpdatedState(onResult)
    // Whether Android would explain itself before this request: false both before and after a refusal means its
    // dialog never showed.
    val explainedBefore = remember { booleanArrayOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && activity != null && !explainedBefore[0] && !activity.explains(permission)) {
            activity.openSettings(settingsIntent(activity))
        }
        currentOnResult(granted)
    }
    return {
        explainedBefore[0] = activity?.explains(permission) == true
        launcher.launch(permission)
    }
}

private fun Activity.explains(permission: String): Boolean = shouldShowRequestPermissionRationale(permission)
