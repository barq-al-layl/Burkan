package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
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
import kotlin.math.roundToInt

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

/**
 * One UI's stepped slider: a short grey track with a dot at each of [count] stops, and a ring in the accent on the
 * stop at [index]. Touching the track takes the ring to the nearest stop, and it follows the finger from there, with
 * a light tick at each stop it lands on.
 *
 * The touch and the drag are one gesture, read by hand: a tap detector and a drag detector side by side each wait
 * to see which of them the touch is, and a slow drag was taken for a tap.
 */
@Composable
fun OneUiStepSlider(count: Int, index: Int, onIndex: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // The ring is filled like the card the slider sits on, wherever that is.
    val cardColor = segmentContainerColor
    val current by rememberUpdatedState(index)
    val choose by rememberUpdatedState(onIndex)
    val haptics = LocalHapticFeedback.current
    Canvas(
        modifier = modifier
            .height(StepThumbSize + 24.dp)
            .semantics {
                stateDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(index.toFloat(), 0f..(count - 1).toFloat(), count - 2)
                setProgress { value ->
                    choose(value.roundToInt().coerceIn(0, count - 1))
                    true
                }
            }
            .pointerInput(count) {
                val inset = StepTrackHeight.toPx() / 2f
                fun pick(x: Float) {
                    val share = ((x - inset) / (size.width - inset * 2)).coerceIn(0f, 1f)
                    val at = (share * (count - 1)).roundToInt()
                    if (at == current) return
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    choose(at)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pick(down.position.x)
                    // Every move is taken, in any direction, so nothing behind the slider scrolls or drags away
                    // while the finger is on it.
                    drag(down.id) { change ->
                        change.consume()
                        pick(change.position.x)
                    }
                }
            },
    ) {
        val track = StepTrackHeight.toPx()
        val middle = size.height / 2f
        val inset = track / 2f
        drawRoundRect(
            color = scheme.onSurface.copy(alpha = STEP_TRACK_ALPHA),
            topLeft = Offset(0f, middle - track / 2f),
            size = size.copy(height = track),
            cornerRadius = CornerRadius(track / 2f),
        )
        fun stop(at: Int) = Offset(inset + (size.width - inset * 2) * at / (count - 1), middle)
        repeat(count) { at -> drawCircle(scheme.onSurfaceVariant.copy(alpha = STEP_DOT_ALPHA), StepDotRadius.toPx(), stop(at)) }
        // The ring: filled like the card under it, so the stop it sits on is covered, with the accent around it.
        val thumb = StepThumbSize.toPx() / 2f
        drawCircle(cardColor, thumb, stop(index))
        drawCircle(scheme.primary, thumb - StepRingWidth.toPx() / 2f, stop(index), style = Stroke(StepRingWidth.toPx()))
    }
}

private val StepTrackHeight = 12.dp
private val StepDotRadius = 3.dp
private val StepThumbSize = 20.dp
private val StepRingWidth = 2.dp
private const val STEP_TRACK_ALPHA = 0.12f
private const val STEP_DOT_ALPHA = 0.7f
private val SearchHeight = 52.dp

/** How much longer a list's end must be for its last rows to clear the floating search. Nothing in Material. */
val FloatingSearchRoom: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) SearchHeight + 24.dp else 0.dp
