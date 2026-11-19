package com.fintrack.app.presentation.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    if (showSyncOffDialog) {
        SyncOffDialog(
            onKeepCloud = {
                showSyncOffDialog = false
                viewModel.disableSyncKeepCloud()
            },
            onDismiss = { showSyncOffDialog = false }
        )
    }

    when (profile.mode) {
        AccountMode.LOCAL_ONLY -> SettingsSection(title = "Account & sync") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Back up your data", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your data is stored only on this phone. Create an account to keep it safe " +
                            "if you lose or replace your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { navController.navigate(AppRoutes.Register.route) }) {
                            Text("Create account")
                        }
                        OutlinedButton(onClick = { navController.navigate(AppRoutes.Login.route) }) {
                            Text("Sign in")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.exportTransactionsCsv() }) {
                        androidx.compose.material3.Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export my transactions (CSV)")
                    }
                }
            }
        }

        AccountMode.ACCOUNT -> SettingsSection(title = "Account & sync") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (viewModel.email.isNotBlank()) {
                        Text(viewModel.email, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Cloud sync", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (profile.cloudSyncEnabled) "Your data is backed up and synced"
                                    else "Off: your data stays on this phone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        androidx.compose.material3.Switch(
                            checked = profile.cloudSyncEnabled,
                            onCheckedChange = { on ->
                                if (on) viewModel.enableSync() else showSyncOffDialog = true
                            }
                        )
                    }
                    when (val t = transfer) {
                        is CloudTransferState.Running -> {
                            Spacer(Modifier.height(8.dp))
                            Text("Syncing your data...", style = MaterialTheme.typography.bodySmall)
                        }
                        is CloudTransferState.Failed -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Sync problem: ${t.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        AccountMode.NONE -> {}
    }
}

@Composable
private fun SyncOffDialog(onKeepCloud: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn off Cloud sync?") },
        text = {
            Text(
                "FinTrack will stop uploading and downloading. The data on this phone stays here, " +
                    "and your cloud copy is kept so you can turn sync back on later."
            )
        },
        confirmButton = { TextButton(onClick = onKeepCloud) { Text("Keep my cloud copy") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
