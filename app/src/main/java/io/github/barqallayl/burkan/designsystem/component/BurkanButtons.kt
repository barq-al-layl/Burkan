package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle

/**
 * The shapes of a button [height] tall. In the Material style a button is round at rest and squarer while it is
 * pressed, by the amount Material gives a button of that size. One UI's stays the pill it is.
 */
@Composable
fun burkanButtonShapes(height: Dp = ButtonDefaults.MinHeight): ButtonShapes =
    if (LocalAppStyle.current == AppStyle.OneUi) {
        ButtonDefaults.shapes(shape = CircleShape, pressedShape = CircleShape)
    } else {
        ButtonDefaults.shapesFor(height)
    }

/** The same for a button that is only an icon. */
@Composable
fun burkanIconButtonShapes(): IconButtonShapes =
    if (LocalAppStyle.current == AppStyle.OneUi) {
        IconButtonDefaults.shapes(shape = CircleShape, pressedShape = CircleShape)
    } else {
        IconButtonDefaults.shapes()
    }

/** The height of a screen's main actions: Material's medium button. */
val ActionHeight: Dp = ButtonDefaults.MediumContainerHeight

/**
 * The colours of a quieter button. Material's own in the Material style, from the scheme's secondary container.
 * One UI's secondary buttons are grey whatever the accent is.
 */
@Composable
fun burkanTonalButtonColors(): ButtonColors =
    if (LocalAppStyle.current == AppStyle.OneUi) {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        ButtonDefaults.filledTonalButtonColors()
    }

/**
 * The room beside the label of a button of the ordinary size. Material now gives it 16 dp and no longer recommends
 * the 24 dp it used to; One UI's pills keep the wider one.
 */
private val buttonPadding: PaddingValues
    @Composable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) {
        ButtonDefaults.ContentPadding
    } else {
        ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight)
    }

/** A filled button of the ordinary size: the one thing to do next. */
@Composable
fun BurkanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        shapes = burkanButtonShapes(),
        modifier = modifier,
        enabled = enabled,
        contentPadding = buttonPadding,
        content = content,
    )
}

/** A quieter button of the ordinary size, for what matters less than the filled one beside or above it. */
@Composable
fun BurkanTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors = burkanTonalButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        shapes = burkanButtonShapes(),
        modifier = modifier,
        colors = colors,
        contentPadding = buttonPadding,
        content = content,
    )
}

/** A button with no container: the least of several things to do, and a dialog's answers. */
@Composable
fun BurkanTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick = onClick, shapes = burkanButtonShapes(), modifier = modifier, content = content)
}
