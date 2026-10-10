package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A button that is only an icon. [label] names it twice over: to a screen reader, and to anyone who touches and
 * holds it, in a small bubble under the button. An icon alone is a guess until it has been tried.
 */
@Composable
fun BurkanIconButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BurkanTooltip(label) {
        IconButton(onClick = onClick, shapes = burkanIconButtonShapes(), modifier = modifier) {
            Icon(icon, contentDescription = label)
        }
    }
}

/** Shows [label] in a bubble under [content] while it is touched and held. */
@Composable
fun BurkanTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content,
    )
}
