package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Search
import com.composables.icons.tabler.outline.X
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle

/**
 * One UI's switch: a small pill that fills with the accent, and a white thumb that slides across it. It only shows
 * [checked]; the row it sits in is what is tapped. Material's own switch is half as large again, which in a One UI
 * list crowds the text beside it.
 */
@Composable
fun OneUiSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switch",
    )
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.size(width = SwitchWidth, height = SwitchHeight)) {
        drawRoundRect(lerp(off, on, position), cornerRadius = CornerRadius(size.height / 2f))
        val radius = size.height / 2f - SwitchThumbInset.toPx()
        val start = size.height / 2f
        val end = size.width - size.height / 2f
        drawCircle(Color.White, radius, Offset(lerp(start, end, position), size.height / 2f))
    }
}

private val SwitchWidth = 34.dp
private val SwitchHeight = 20.dp
private val SwitchThumbInset = 2.5.dp

/**
 * One UI's search: a pill floating over the foot of a list, in reach of the thumb, rather than a field at the
 * list's head. It is short while it only waits, and takes the width of the screen once it is typed in. Place it
 * over the list, at the bottom, and give the list [FloatingSearchRoom] more at its end so the last rows clear it.
 */
@Composable
fun BurkanFloatingSearch(
    query: String,
    onSearch: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val share by animateFloatAsState(
        targetValue = if (focused || query.isNotEmpty()) 1f else RESTING_SHARE,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "searchWidth",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(share)
                .height(SearchHeight),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            // A faint rim and a shadow: what lifts it off the rows that pass beneath.
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Tabler.Outline.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = query,
                    onValueChange = onSearch,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focused = it.isFocused },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { field ->
                        if (query.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        field()
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(
                            Tabler.Outline.X,
                            contentDescription = clearLabel,
                        )
                    }
                }
            }
        }
    }
}

/** The share of the screen's width the pill takes while it waits. */
private const val RESTING_SHARE = 0.68f
private val SearchHeight = 52.dp

/** How much longer a list's end must be for its last rows to clear the floating search. Nothing in Material. */
val FloatingSearchRoom: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) SearchHeight + 24.dp else 0.dp
