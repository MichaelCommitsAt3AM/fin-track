package com.fintrack.app.presentation.settings.mpesa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.app.core.backup.BackupAvailability
import java.text.DateFormat
import java.util.Date

private const val MIN_PASSPHRASE = 8

/** Settings, M-Pesa, Cloud backup. Personal flavor only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaBackupScreen(
    onNavigateBack: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    viewModel: MpesaBackupViewModel = hiltViewModel()
) {
    val status by viewModel.status.collectAsState()
    val ui by viewModel.ui.collectAsState()
    var showConsent by remember { mutableStateOf(false) }
    var showTurnOff by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var showChange by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    LaunchedEffect(status.availability) { viewModel.refresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Cloud backup") },
                navigationIcon = {
                    IconButton(onClick = { if (showConsent) showConsent = false else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ui.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ui.info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            ui.restoreResult?.let {
                Text(
                    "Restored ${it.transactions} transactions" +
                        (if (it.categoryChoices > 0) " and ${it.categoryChoices} category choices" else "") +
                        (if (it.skipped > 0) " (${it.skipped} records could not be read)" else "") + ".",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            when {
                status.availability == BackupAvailability.NEEDS_ACCOUNT -> NeedsAccountCard(onOpenAccountSettings)
                showConsent -> ConsentForm(
                    existingBackup = ui.cloudBackupExists == true,
                    busy = ui.busy,
                    onTurnOn = { pass -> viewModel.enable(pass) { showConsent = false } }
                )
                !status.enabled -> {
                    IntroCard(onTurnOn = { viewModel.clearMessages(); showConsent = true })
                    if (ui.cloudBackupExists == true) {
                        BackupFoundCard(
                            onRestore = { viewModel.clearMessages(); showRestore = true },
                            onReset = { showReset = true }
                        )
                    }
                }
                else -> EnabledCard(
                    lastBackupAt = status.lastBackupAt,
                    lastError = status.lastError,
                    busy = ui.busy,
                    onBackupNow = { viewModel.backupNow() },
                    onChangePassphrase = { showChange = true },
                    onRestore = { viewModel.clearMessages(); showRestore = true },
                    onReset = { showReset = true },
                    onTurnOff = { showTurnOff = true }
                )
            }
        }
    }

    if (showTurnOff) {
        AlertDialog(
            onDismissRequest = { showTurnOff = false },
            title = { Text("Turn off Cloud backup?") },
            text = { Text("You can delete your encrypted backup from the cloud, or keep it so you can restore from it later.") },
            confirmButton = {
                TextButton(onClick = { showTurnOff = false; viewModel.disable(deleteCloud = true) }) {
                    Text("Delete my backup from the cloud")
                }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { showTurnOff = false; viewModel.disable(deleteCloud = false) }) { Text("Keep it") }
                    TextButton(onClick = { showTurnOff = false }) { Text("Cancel") }
                }
            }
        )
    }
    if (showRestore || ui.restoreProgress != null) {
        RestoreDialog(viewModel = viewModel, onDismiss = { showRestore = false })
    }
    if (showChange) {
        ChangePassphraseDialog(
            busy = ui.busy,
            onChange = { old, new -> viewModel.changePassphrase(old, new) { showChange = false } },
            onDismiss = { showChange = false }
        )
    }
    if (showReset) {
        ResetDialog(
            onReset = { new -> showReset = false; viewModel.resetBackup(new) },
            onDismiss = { showReset = false }
        )
    }
}

@Composable
private fun NeedsAccountCard(onOpenAccountSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Requires an account", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cloud backup keeps your M-Pesa history safe if you lose or replace your phone. " +
                    "It needs an account to store the encrypted backup.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onOpenAccountSettings) { Text("Go to Account & sync") }
        }
    }
}

@Composable
private fun IntroCard(onTurnOn: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cloud backup", style = MaterialTheme.typography.titleMedium)
                    Text("Off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = false, onCheckedChange = { onTurnOn() })
            }
            Text(
                "Back up your M-Pesa history, end-to-end encrypted with a passphrase only you know, " +
                    "so it survives a lost or replaced phone. Your SMS messages are never uploaded.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BackupFoundCard(onRestore: () -> Unit, onReset: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("A backup was found for your account", style = MaterialTheme.typography.titleMedium)
            Text("Restore your M-Pesa history and category choices with your backup passphrase.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRestore) { Text("Restore your M-Pesa history") }
            TextButton(onClick = onReset) { Text("Forgot the passphrase? Reset backup") }
        }
    }
}

@Composable
private fun EnabledCard(
    lastBackupAt: Long,
    lastError: String?,
    busy: Boolean,
    onBackupNow: () -> Unit,
    onChangePassphrase: () -> Unit,
    onRestore: () -> Unit,
    onReset: () -> Unit,
    onTurnOff: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cloud backup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (lastBackupAt > 0) "On. Last backup ${DateFormat.getDateTimeInstance().format(Date(lastBackupAt))}"
                        else "On. Waiting for the first backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = true, onCheckedChange = { onTurnOff() })
            }
            lastError?.let { Text("Last attempt failed: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackupNow, enabled = !busy) { Text("Back up now") }
                OutlinedButton(onClick = onChangePassphrase, enabled = !busy) { Text("Change passphrase") }
            }
            TextButton(onClick = onRestore) { Text("Restore from backup") }
            TextButton(onClick = onReset) { Text("Forgot the passphrase? Reset backup") }
        }
    }
}

@Composable
private fun ConsentForm(existingBackup: Boolean, busy: Boolean, onTurnOn: (CharArray) -> Unit) {
    var understood by remember { mutableStateOf(false) }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = understood && pass.length >= MIN_PASSPHRASE && pass == confirm

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Before you turn this on", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        ConsentBlock(
            "What is uploaded",
            "Receipt number, amount, date, type, transaction kind, merchant name, paybill or till number, " +
                "account number, transaction cost, and the categories you chose for M-Pesa transactions."
        )
        ConsentBlock("What is never uploaded", "The SMS text, M-Pesa balances, and phone numbers.")
        ConsentBlock(
            "How it is protected",
            "It is encrypted on this phone with a backup passphrase that only you know. FinTrack cannot read it."
        )
        ConsentBlock(
            "The trade-off",
            "If you forget the passphrase, the backup cannot be restored. Nobody can recover it for you."
        )
        if (existingBackup) {
            Text(
                "A backup already exists for this account. Enter its passphrase to continue using it " +
                    "(or go back and reset the backup).",
                color = MaterialTheme.colorScheme.primary
            )
        }
        PassphraseField("Backup passphrase (at least $MIN_PASSPHRASE characters)", pass) { pass = it }
        PassphraseField("Repeat passphrase", confirm) { confirm = it }
        if (confirm.isNotEmpty() && pass != confirm) {
            Text("The passphrases don't match.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = understood, onCheckedChange = { understood = it })
            Text("I understand that if I forget the passphrase, my backup can't be restored.", Modifier.weight(1f))
        }
        Button(onClick = { onTurnOn(pass.toCharArray()) }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Turn on Cloud backup")
        }
    }
}

@Composable
private fun ConsentBlock(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PassphraseField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Restore from the backup. Also used from the M-Pesa onboarding offer. */
@Composable
fun RestoreDialog(viewModel: MpesaBackupViewModel, onDismiss: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var pass by remember { mutableStateOf("") }
    val progress = ui.restoreProgress

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("Restore your M-Pesa history") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    progress != null -> {
                        val (done, total) = progress
                        if (total != null && total > 0) LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Restoring... $done${if (total != null) " of $total" else ""}")
                    }
                    ui.restoreResult != null -> {
                        val r = ui.restoreResult!!
                        Text("Restored ${r.transactions} transactions.")
                    }
                    else -> {
                        Text("Enter the passphrase you set when you turned on Cloud backup.")
                        PassphraseField("Backup passphrase", pass) { pass = it }
                        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            if (ui.restoreResult != null) {
                TextButton(onClick = { viewModel.clearMessages(); onDismiss() }) { Text("Done") }
            } else {
                TextButton(onClick = { viewModel.restore(pass.toCharArray()) }, enabled = pass.isNotEmpty() && progress == null && !ui.busy) {
                    Text("Restore")
                }
            }
        },
        dismissButton = {
            if (ui.restoreResult == null && progress == null) {
                TextButton(onClick = { viewModel.clearMessages(); viewModel.dismissOffer(); onDismiss() }) { Text("Not now") }
            }
        }
    )
}

@Composable
private fun ChangePassphraseDialog(busy: Boolean, onChange: (CharArray, CharArray) -> Unit, onDismiss: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PassphraseField("Current passphrase", old) { old = it }
                PassphraseField("New passphrase (at least $MIN_PASSPHRASE characters)", new) { new = it }
                PassphraseField("Repeat new passphrase", confirm) { confirm = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onChange(old.toCharArray(), new.toCharArray()) },
                enabled = !busy && old.isNotEmpty() && new.length >= MIN_PASSPHRASE && new == confirm
            ) { Text("Change") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ResetDialog(onReset: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var understood by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This permanently deletes your existing cloud backup, because without the old passphrase " +
                        "it cannot be read. A new backup is then made from the M-Pesa data on this phone, " +
                        "protected by the new passphrase.",
                    style = MaterialTheme.typography.bodyMedium
                )
                PassphraseField("New passphrase (at least $MIN_PASSPHRASE characters)", new) { new = it }
                PassphraseField("Repeat new passphrase", confirm) { confirm = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = understood, onCheckedChange = { understood = it })
                    Text("Delete my old backup", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onReset(new.toCharArray()) },
                enabled = understood && new.length >= MIN_PASSPHRASE && new == confirm
            ) { Text("Reset backup") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
