package com.fintrack.app.presentation.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import com.fintrack.app.core.ui.components.AppActionSheet
import com.fintrack.app.core.ui.components.SheetAction
import com.fintrack.app.core.ui.components.SheetActionKind

/** Shown when the account being linked already has FinTrack data in the cloud. */
@Composable
fun CombineDataDialog(viewModel: AuthViewModel) {
    AppActionSheet(
        title = "Combine your data?",
        icon = Icons.Default.CloudSync,
        heading = "This account already has data",
        message = "This account already has FinTrack data in the cloud. Combine it with the data " +
            "on this phone? Nothing is deleted either way.",
        onDismiss = { viewModel.cancelCombine() },
        actions = listOf(
            SheetAction("Cancel", SheetActionKind.SECONDARY) { viewModel.cancelCombine() },
            SheetAction("Combine") { viewModel.confirmCombine() }
        )
    )
}

/** Confirmation before starting without an account. */
@Composable
fun LocalOnlyConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppActionSheet(
        title = "Continue without an account?",
        icon = Icons.Default.PhoneAndroid,
        heading = "Stored on this phone only",
        message = "Your data will be stored only on this phone. If you lose the phone or uninstall " +
            "the app, it's gone, unless you export it or create an account later. " +
            "You can create an account any time in Settings.",
        onDismiss = onDismiss,
        actions = listOf(
            SheetAction("Cancel", SheetActionKind.SECONDARY, onClick = onDismiss),
            SheetAction("Continue", onClick = onConfirm)
        )
    )
}
