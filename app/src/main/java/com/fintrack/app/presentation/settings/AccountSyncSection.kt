package com.fintrack.app.presentation.settings

import com.fintrack.app.core.ui.components.SheetTone
import com.fintrack.app.core.ui.components.SheetActionKind
import com.fintrack.app.core.ui.components.SheetAction
import com.fintrack.app.core.ui.components.SettingsRowDivider
import com.fintrack.app.core.ui.components.SettingsDetailItem
import com.fintrack.app.core.ui.components.DetailTrailing
import com.fintrack.app.core.ui.components.AppActionSheet
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Login
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.fintrack.app.core.profile.AccountMode
import com.fintrack.app.core.profile.CloudTransferState
import com.fintrack.app.presentation.navigation.AppRoutes

/**
 * "Account & sync" settings: back-up card for local-only users; email, Cloud sync switch and
 * data export for signed-in users.
 */
@Composable
fun AccountSyncSection(
    navController: NavController,
    viewModel: AccountSyncViewModel = hiltViewModel()
) {
    val profile by viewModel.profileState.collectAsState()
    val transfer by viewModel.transferState.collectAsState()
    val context = LocalContext.current
    var showSyncOffDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { uri ->
            if (uri != null) {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "FinTrack Data Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Export Data"))
            } else {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val deleteState by viewModel.deleteState.collectAsState()
    if (showSyncOffDialog) {
        AppActionSheet(
            title = "Turn off Cloud sync?",
            icon = Icons.Default.CloudOff,
            heading = "Your phone keeps its data",
            message = "FinTrack will stop uploading and downloading. The data on this phone stays " +
                "here either way. Keep your cloud copy so you can turn sync back on later, or " +
                "delete it from the cloud.",
            onDismiss = { showSyncOffDialog = false },
            actions = listOf(
                SheetAction("Keep my cloud copy") {
                    showSyncOffDialog = false
                    viewModel.disableSyncKeepCloud()
                },
                SheetAction("Delete my cloud data", SheetActionKind.DANGER) {
                    showSyncOffDialog = false
                    viewModel.disableSyncAndDeleteCloud()
                },
                SheetAction("Cancel", SheetActionKind.TEXT) { showSyncOffDialog = false }
            )
        )
    }
    when (val d = deleteState) {
        is DeleteState.Running -> AppActionSheet(
            title = "Deleting your cloud data",
            icon = Icons.Default.CloudOff,
            heading = d.step,
            onDismiss = {},
            dismissible = false,
            actions = emptyList()
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        is DeleteState.Failed -> AppActionSheet(
            title = "Couldn't delete",
            icon = Icons.Default.ErrorOutline,
            heading = "Your cloud data wasn't deleted",
            message = d.message,
            tone = SheetTone.DANGER,
            onDismiss = { viewModel.dismissDeleteState() },
            actions = listOf(
                SheetAction("Close", SheetActionKind.SECONDARY) { viewModel.dismissDeleteState() },
                SheetAction("Try again") { viewModel.disableSyncAndDeleteCloud() }
            )
        )
        DeleteState.Idle -> {}
    }

    when (profile.mode) {
        AccountMode.LOCAL_ONLY -> SettingsSection(title = "Account & sync") {
            SettingsDetailItem(
                icon = Icons.Default.PhoneAndroid,
                title = "Stored on this phone only",
                subtitle = "Create an account to keep your data safe if you lose or replace your phone",
                trailing = DetailTrailing.None
            )
            SettingsRowDivider()
            SettingsDetailItem(
                icon = Icons.Default.PersonAdd,
                title = "Create account",
                onClick = { navController.navigate(AppRoutes.Register.route) }
            )
            SettingsRowDivider()
            SettingsDetailItem(
                icon = Icons.AutoMirrored.Filled.Login,
                title = "Sign in",
                onClick = { navController.navigate(AppRoutes.Login.route) }
            )
            SettingsRowDivider()
            SettingsDetailItem(
                icon = Icons.Default.FileDownload,
                title = "Export my transactions",
                subtitle = "Save a CSV copy",
                onClick = { viewModel.exportTransactionsCsv() }
            )
        }

        AccountMode.ACCOUNT -> SettingsSection(title = "Account & sync") {
            if (viewModel.email.isNotBlank()) {
                SettingsDetailItem(
                    icon = Icons.Default.AccountCircle,
                    title = viewModel.email,
                    subtitle = "Signed in",
                    trailing = DetailTrailing.None
                )
                SettingsRowDivider()
            }
            val syncSubtitle = when (val t = transfer) {
                is CloudTransferState.Running -> "Syncing your data..."
                is CloudTransferState.Failed -> "Sync problem: ${t.message}"
                else -> if (profile.cloudSyncEnabled) "Your data is backed up and synced"
                else "Off: your data stays on this phone"
            }
            SettingsDetailItem(
                icon = Icons.Default.CloudSync,
                title = "Cloud sync",
                subtitle = syncSubtitle,
                trailing = DetailTrailing.Toggle(
                    checked = profile.cloudSyncEnabled,
                    onCheckedChange = { on -> if (on) viewModel.enableSync() else showSyncOffDialog = true }
                )
            )
        }

        AccountMode.NONE -> {}
    }
}
