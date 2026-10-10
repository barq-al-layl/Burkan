package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Keeps a screen's content to a width that is still easy to read, and centres it. On a phone held upright it does
 * nothing: the screen is narrower than this. Held sideways, or in a wide window, rows would otherwise stretch from
 * edge to edge with a name at one end and its value far away at the other.
 *
 * Put it last on whatever holds the content, after any scrolling: what scrolls stays as wide as the screen, so a
 * swipe anywhere still scrolls it.
 */
fun Modifier.readableWidth(): Modifier =
    wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = ReadableWidth)

/** Material's compact windows end at 600 dp; a single pane much wider than that is past a comfortable line. */
private val ReadableWidth = 640.dp
