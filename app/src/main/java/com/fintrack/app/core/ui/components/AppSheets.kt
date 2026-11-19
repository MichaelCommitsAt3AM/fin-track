package com.fintrack.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/*
 * Shared building blocks that reproduce the app's existing UI patterns, so new screens
 * look like the rest of FinTrack:
 *  - AppActionSheet: same layout as the theme/currency/lookback bottom sheets
 *    (surfaceContainerLow, drag handle, bold title, tinted info card, paired buttons)
 *  - AppInfoCard: the tinted card used inside those sheets
 *  - AppPasswordField: same look as the login screen's text fields
 *  - SettingsDetailItem: SettingsItem with a subtitle line
 */

enum class SheetTone { INFO, DANGER }

enum class SheetActionKind { PRIMARY, SECONDARY, DANGER, TEXT }

data class SheetAction(
    val label: String,
    val kind: SheetActionKind = SheetActionKind.PRIMARY,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun toneColor(tone: SheetTone) =
    if (tone == SheetTone.DANGER) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

@Composable
private fun onToneColor(tone: SheetTone) =
    if (tone == SheetTone.DANGER) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary

/** Tinted card with an icon box, heading and message, as used in the app's bottom sheets. */
@Composable
fun AppInfoCard(
    icon: ImageVector,
    heading: String,
    message: String? = null,
    tone: SheetTone = SheetTone.INFO,
    modifier: Modifier = Modifier
) {
    val color = toneColor(tone)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = onToneColor(tone), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            if (message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bottom sheet in the app's standard style. [content] goes between the info card and the
 * buttons (e.g. text fields or progress). Set [dismissible] to false while work is running.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    title: String,
    icon: ImageVector,
    heading: String,
    message: String? = null,
    onDismiss: () -> Unit,
    actions: List<SheetAction>,
    tone: SheetTone = SheetTone.INFO,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { dismissible || it != SheetValue.Hidden }
    )
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )

            AppInfoCard(icon = icon, heading = heading, message = message, tone = tone)

            if (content != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }

            if (actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                SheetActions(actions)
            }
        }
    }
}

@Composable
private fun SheetActions(actions: List<SheetAction>) {
    // Two buttons sit side by side (secondary left, primary right), like the lookback sheet;
    // three or more stack full-width so long labels never wrap awkwardly.
    if (actions.size == 2 && actions.none { it.kind == SheetActionKind.TEXT }) {
        val ordered = actions.sortedBy { if (it.kind == SheetActionKind.SECONDARY) 0 else 1 }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ordered.forEach { SheetActionButton(it, Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            actions.forEach { SheetActionButton(it, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun SheetActionButton(action: SheetAction, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val sized = modifier.heightIn(min = 48.dp)
    when (action.kind) {
        SheetActionKind.PRIMARY -> Button(
            onClick = action.onClick, enabled = action.enabled, shape = shape, modifier = sized
        ) { Text(action.label, fontWeight = FontWeight.Bold) }

        SheetActionKind.DANGER -> Button(
            onClick = action.onClick, enabled = action.enabled, shape = shape, modifier = sized,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) { Text(action.label, fontWeight = FontWeight.Bold) }

        SheetActionKind.SECONDARY -> OutlinedButton(
            onClick = action.onClick, enabled = action.enabled, shape = shape, modifier = sized
        ) { Text(action.label) }

        SheetActionKind.TEXT -> TextButton(
            onClick = action.onClick, enabled = action.enabled, shape = shape, modifier = sized
        ) { Text(action.label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Password/passphrase field styled like the login screen's fields. */
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

/** Trailing content for [SettingsDetailItem]. */
sealed interface DetailTrailing {
    data object Chevron : DetailTrailing
    data object None : DetailTrailing
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit, val enabled: Boolean = true) : DetailTrailing
}

/**
 * Same layout as the settings screen's SettingsItem (40dp icon box, medium title, chevron or
 * switch), with an optional subtitle and a danger tint for destructive rows.
 */
@Composable
fun SettingsDetailItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: DetailTrailing = DetailTrailing.Chevron,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val accent = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val clickable = onClick != null && trailing !is DetailTrailing.Toggle && enabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick?.invoke() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    danger -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when (trailing) {
            DetailTrailing.Chevron -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DetailTrailing.None -> {}
            is DetailTrailing.Toggle -> Switch(
                checked = trailing.checked,
                onCheckedChange = trailing.onCheckedChange,
                enabled = trailing.enabled && enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

/** Divider used between rows inside a settings section, matching the M-Pesa settings screen. */
@Composable
fun SettingsRowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** Inline status banner (success / error) in the same tinted style as the info cards. */
@Composable
fun AppStatusBanner(text: String, isError: Boolean, icon: ImageVector) {
    AppInfoCard(
        icon = icon,
        heading = text,
        tone = if (isError) SheetTone.DANGER else SheetTone.INFO
    )
}

