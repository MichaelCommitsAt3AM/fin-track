package com.fintrack.app.presentation.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Shown when the account being linked already has FinTrack data in the cloud. */
@Composable
fun CombineDataDialog(viewModel: AuthViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.cancelCombine() },
        title = { Text("Combine your data?") },
        text = {
            Text(
                "This account already has FinTrack data. Combine it with the data on this phone? " +
                    "Nothing is deleted either way."
            )
        },
        confirmButton = { TextButton(onClick = { viewModel.confirmCombine() }) { Text("Combine") } },
        dismissButton = { TextButton(onClick = { viewModel.cancelCombine() }) { Text("Cancel") } }
    )
}

/** Confirmation before starting without an account. */
@Composable
fun LocalOnlyConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Continue without an account?") },
        text = {
            Text(
                "Your data will be stored only on this phone. If you lose the phone or uninstall " +
                    "the app, it's gone, unless you export it or create an account later. " +
                    "You can create an account any time in Settings."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
