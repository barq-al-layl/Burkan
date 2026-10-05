package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import kotlin.math.roundToInt
import org.orbitmvi.orbit.compose.collectAsState

/**
 * The text size as a screen of its own, the way One UI's Settings sets it: a sample at the top, and the slider on
 * a card at the foot of the screen, in reach. A step applies at once to the whole app, this screen included. Sizes
 * of text are in sp and everything else here is in dp, so the slider stays under the finger as the text around it
 * grows.
 */
@Composable
fun TextSizeScreen() {
    val viewModel = metroViewModel<SettingsViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    val percent =
        state.values?.appearance?.textScalePercent ?: SettingsStorage.Defaults.TEXT_SCALE_PERCENT
    TextSizeContent(
        percent = percent,
        onPercent = viewModel::setTextScalePercent,
        onBack = navigator::pop,
    )
}

@Composable
private fun TextSizeContent(percent: Int, onPercent: (Int) -> Unit, onBack: () -> Unit) {
    val steps = TextScale.percentages.toList()
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            BurkanTopBar(
                title = { Text(stringResource(R.string.settings_text_size)) },
                onBack = onBack,
                contentScroll = { scrollState.value },
            )
        },
        bottomBar = {
            BurkanSegment(
                index = 0,
                count = 1,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = ScreenMargin)
                    .padding(bottom = GroupGap),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SLIDER_CARD_HEIGHT)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A small letter and a large one at the ends: which way is which. Their own sizes are fixed,
                    // so they do not move the slider as the setting changes.
                    FixedLetter(SMALL_LETTER)
                    StepSlider(
                        count = steps.size,
                        index = steps.indexOf(percent).coerceAtLeast(0),
                        onIndex = { onPercent(steps[it]) },
                        label = textSizeLabel(percent),
                        modifier = Modifier.weight(1f),
                    )
                    FixedLetter(LARGE_LETTER)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.listTop(),
                    bottom = innerPadding.calculateBottomPadding(),
                )
                .oneUiScrollFade(scrollState)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            BurkanSegment(index = 0, count = 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SAMPLE_HEIGHT)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.text_size_sample),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            val isDefault = percent == SettingsStorage.Defaults.TEXT_SCALE_PERCENT
            BurkanSegmentItem(
                index = 0,
                count = 1,
                headline = stringResource(R.string.text_size_current),
                onClick = if (isDefault) null else ({ onPercent(SettingsStorage.Defaults.TEXT_SCALE_PERCENT) }),
                // What is set now, in the accent, as One UI shows a row's value; a tap puts the default back.
                content = {
                    Text(
                        if (isDefault) {
                            stringResource(R.string.text_size_default)
                        } else {
                            stringResource(
                                R.string.text_size_tap_to_reset,
                                stringResource(R.string.text_size_percent, percent),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
    }
}

/** A letter whose size is set in dp, so the text size being chosen does not change it. */
@Composable
private fun FixedLetter(size: Dp) {
    Text(
        "A",
        fontSize = with(LocalDensity.current) { size.toSp() },
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clearAndSetSemantics { },
    )
}

/**
 * One UI's stepped slider: a short grey track with a dot at each stop, and a ring in the accent on the stop that
 * is chosen. A tap or a drag takes the ring to the nearest stop.
 */
@Composable
private fun StepSlider(
    count: Int,
    index: Int,
    onIndex: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val current by rememberUpdatedState(index)
    val choose by rememberUpdatedState(onIndex)
    fun indexAt(x: Float, width: Float, inset: Float): Int {
        val share = ((x - inset) / (width - inset * 2)).coerceIn(0f, 1f)
        return (share * (count - 1)).roundToInt()
    }
    Canvas(
        modifier = modifier
            .height(THUMB_SIZE + 16.dp)
            .semantics {
                stateDescription = label
                progressBarRangeInfo =
                    ProgressBarRangeInfo(index.toFloat(), 0f..(count - 1).toFloat(), count - 2)
                setProgress { value ->
                    choose(value.roundToInt().coerceIn(0, count - 1))
                    true
                }
            }
            .pointerInput(count) {
                detectTapGestures { at ->
                    choose(
                        indexAt(
                            at.x,
                            size.width.toFloat(),
                            TRACK_HEIGHT.toPx() / 2f,
                        ),
                    )
                }
            }
            .pointerInput(count) {
                detectHorizontalDragGestures { change, _ ->
                    val at =
                        indexAt(change.position.x, size.width.toFloat(), TRACK_HEIGHT.toPx() / 2f)
                    if (at != current) choose(at)
                }
            },
    ) {
        val track = TRACK_HEIGHT.toPx()
        val middle = size.height / 2f
        val inset = track / 2f
        drawRoundRect(
            color = scheme.surfaceContainerHighest,
            topLeft = Offset(0f, middle - track / 2f),
            size = size.copy(height = track),
            cornerRadius = CornerRadius(track / 2f),
        )
        fun stop(at: Int) = Offset(inset + (size.width - inset * 2) * at / (count - 1), middle)
        repeat(count) { at ->
            drawCircle(
                scheme.onSurfaceVariant.copy(alpha = DOT_ALPHA),
                STOP_RADIUS.toPx(),
                stop(at),
            )
        }
        // The ring: the card's own colour inside, so the stop under it is covered, and the accent around it.
        val thumb = THUMB_SIZE.toPx() / 2f
        drawCircle(scheme.surfaceContainer, thumb, stop(index))
        drawCircle(
            scheme.primary,
            thumb - RING_WIDTH.toPx() / 2f,
            stop(index),
            style = Stroke(RING_WIDTH.toPx()),
        )
    }
}

private val SAMPLE_HEIGHT = 260.dp
private val SLIDER_CARD_HEIGHT = 70.dp
private val SMALL_LETTER = 15.dp
private val LARGE_LETTER = 24.dp
private val TRACK_HEIGHT = 12.dp
private val STOP_RADIUS = 3.dp
private val THUMB_SIZE = 20.dp
private val RING_WIDTH = 2.dp
private const val DOT_ALPHA = 0.7f

@BurkanPreview
@Composable
private fun TextSizeScreenOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        TextSizeContent(percent = 90, onPercent = {}, onBack = {})
    }
}

@BurkanPreview
@Composable
private fun TextSizeScreenOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        TextSizeContent(percent = 100, onPercent = {}, onBack = {})
    }
}
