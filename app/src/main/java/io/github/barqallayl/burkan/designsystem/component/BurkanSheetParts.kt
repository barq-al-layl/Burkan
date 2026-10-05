package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.tabler.Tabler
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import com.composables.icons.tabler.outline.Search
import com.composables.icons.tabler.outline.X

/** A sheet's title, with an optional badge ahead of it and something [trailing] at the end: a value, a count. */
@Composable
fun BurkanSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) BurkanIconBadge(icon, tone = Tone.Good)
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * The two buttons that end a sheet, side by side and equally wide: the way out, quiet, then the action, filled.
 * Either is left out when its label is null.
 */
@Composable
fun BurkanSheetActions(
    modifier: Modifier = Modifier,
    dismiss: String? = null,
    onDismiss: () -> Unit = {},
    dismissIcon: ImageVector? = Tabler.Outline.X,
    confirm: String? = null,
    onConfirm: () -> Unit = {},
    confirmIcon: ImageVector? = null,
    confirmEnabled: Boolean = true,
) {
    // One UI's dialogs end in flat text buttons, parted by a short line: no filled shapes on the card.
    if (LocalAppStyle.current == AppStyle.OneUi) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(SheetActionHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { FlatLabel(dismiss) }
            }
            if (dismiss != null && confirm != null) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            if (confirm != null) {
                TextButton(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    FlatLabel(confirm)
                }
            }
        }
        return
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (dismiss != null) {
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(SheetActionHeight),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                ActionLabel(dismiss, dismissIcon)
            }
        }
        if (confirm != null) {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(SheetActionHeight),
            ) {
                ActionLabel(confirm, confirmIcon)
            }
        }
    }
}

@Composable
private fun ActionLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, maxLines = 1)
}

@Composable
private fun FlatLabel(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

private val SheetActionHeight = 52.dp

/**
 * A question that needs an answer before the app goes on: a title, a sentence, and two ways out. Material asks it
 * in a dialog in the middle of the screen. One UI asks at the bottom, within reach, on the app's floating sheet.
 */
@Composable
fun BurkanConfirm(
    title: String,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (LocalAppStyle.current == AppStyle.OneUi) {
        BurkanBottomSheet(onDismiss = onDismiss) { hide ->
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BurkanSheetHeader(title)
                Text(text, style = MaterialTheme.typography.bodyMedium)
                BurkanSheetActions(
                    dismiss = dismiss,
                    onDismiss = { hide(onDismiss) },
                    confirm = confirm,
                    onConfirm = { hide(onConfirm) },
                )
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
        )
    }
}

/** A pill-shaped search field in the segments' colour, with a button to empty it. */
@Composable
fun BurkanSearchField(
    query: String,
    onSearch: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onSearch,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Tabler.Outline.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onSearch("") }) {
                    Icon(
                        Tabler.Outline.X,
                        contentDescription = clearLabel,
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = segmentContainerColor,
            unfocusedContainerColor = segmentContainerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
