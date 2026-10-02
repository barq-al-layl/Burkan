package io.github.barqallayl.burkan.designsystem.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.barqallayl.burkan.R

/** The launcher icon's two layers, under a round mask and a rounded-square one, so a change to it shows here. */
@BurkanPreview
@Composable
private fun AppIconPreview() {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AdaptiveIcon(CircleShape)
        AdaptiveIcon(RoundedCornerShape(28.dp))
    }
}

@Composable
private fun AdaptiveIcon(mask: Shape) {
    // A launcher shows the middle 72 of the 108 dp layers; the rest is for its motion effects.
    Box(modifier = Modifier.size(144.dp).clip(mask), contentAlignment = Alignment.Center) {
        listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { layer ->
            Image(painterResource(layer), contentDescription = null, modifier = Modifier.requiredSize(216.dp))
        }
    }
}
