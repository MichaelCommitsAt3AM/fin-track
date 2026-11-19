package com.fintrack.app.presentation.settings.mpesa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fintrack.app.core.backup.BackupAvailability
import com.fintrack.app.core.ui.components.AppActionSheet
import com.fintrack.app.core.ui.components.AppInfoCard
import com.fintrack.app.core.ui.components.AppPasswordField
import com.fintrack.app.core.ui.components.DetailTrailing
import com.fintrack.app.core.ui.components.SettingsDetailItem
import com.fintrack.app.core.ui.components.SettingsRowDivider
import com.fintrack.app.core.ui.components.SheetAction
import com.fintrack.app.core.ui.components.SheetActionKind
import com.fintrack.app.core.ui.components.SheetTone
import com.fintrack.app.presentation.settings.SettingsSection
import java.text.DateFormat
import java.util.Date

private const val MIN_PASSPHRASE = 8

/** Settings, M-Pesa, Cloud backup. Personal flavor only. Styled like MpesaSettingsScreen. */
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
        topBar = {
            TopAppBar(
                title = { Text(if (showConsent) "Turn on Cloud backup" else "Cloud backup") },
                navigationIcon = {
                    IconButton(onClick = { if (showConsent) showConsent = false else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status messages
            if (ui.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            ui.error?.let { AppInfoCard(icon = Icons.Default.ErrorOutline, heading = it, tone = SheetTone.DANGER) }
            ui.info?.let { AppInfoCard(icon = Icons.Default.CheckCircle, heading = it) }
            ui.restoreResult?.let {
                AppInfoCard(
                    icon = Icons.Default.CheckCircle,
                    heading = "Restored ${it.transactions} transactions",
                    message = buildString {
                        if (it.categoryChoices > 0) append("Also restored ${it.categoryChoices} category choices. ")
                        if (it.skipped > 0) append("${it.skipped} records couldn't be read.")
                    }.ifBlank { null }
                )
            }

            when {
                status.availability == BackupAvailability.NEEDS_ACCOUNT -> NeedsAccountContent(onOpenAccountSettings)
                showConsent -> ConsentForm(
                    existingBackup = ui.cloudBackupExists == true,
                    busy = ui.busy,
                    onTurnOn = { pass -> viewModel.enable(pass) { showConsent = false } }
                )
                !status.enabled -> OffContent(
                    backupFound = ui.cloudBackupExists == true,
                    onTurnOn = { viewModel.clearMessages(); showConsent = true },
                    onRestore = { viewModel.clearMessages(); showRestore = true },
                    onReset = { showReset = true }
                )
                else -> EnabledContent(
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
        AppActionSheet(
            title = "Turn off Cloud backup?",
            icon = Icons.Default.CloudOff,
            heading = "What should happen to your backup?",
            message = "You can delete your encrypted backup from the cloud, or keep it so you can " +
                "restore from it later.",
            onDismiss = { showTurnOff = false },
            actions = listOf(
                SheetAction("Delete my backup from the cloud", SheetActionKind.DANGER) {
                    showTurnOff = false; viewModel.disable(deleteCloud = true)
                },
                SheetAction("Keep it", SheetActionKind.SECONDARY) {
                    showTurnOff = false; viewModel.disable(deleteCloud = false)
                },
                SheetAction("Cancel", SheetActionKind.TEXT) { showTurnOff = false }
            )
        )
    }
    if (showRestore || ui.restoreProgress != null) {
        RestoreDialog(viewModel = viewModel, onDismiss = { showRestore = false })
    }
    if (showChange) {
        ChangePassphraseSheet(
            busy = ui.busy,
            onChange = { old, new -> viewModel.changePassphrase(old, new) { showChange = false } },
            onDismiss = { showChange = false }
        )
    }
    if (showReset) {
        ResetSheet(
            onReset = { new -> showReset = false; viewModel.resetBackup(new) },
            onDismiss = { showReset = false }
        )
    }
}

@Composable
private fun NeedsAccountContent(onOpenAccountSettings: () -> Unit) {
    AppInfoCard(
        icon = Icons.Default.ManageAccounts,
        heading = "Requires an account",
        message = "Cloud backup keeps your M-Pesa history safe if you lose or replace your phone. " +
            "It needs an account to store the encrypted backup."
    )
    Button(
        onClick = onOpenAccountSettings,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) { Text("Go to Account & sync", fontWeight = FontWeight.Bold) }
}

@Composable
private fun OffContent(
    backupFound: Boolean,
    onTurnOn: () -> Unit,
    onRestore: () -> Unit,
    onReset: () -> Unit
) {
    SettingsSection(title = "Backup") {
        SettingsDetailItem(
            icon = Icons.Default.CloudUpload,
            title = "Cloud backup",
            subtitle = "Off",
            trailing = DetailTrailing.Toggle(checked = false, onCheckedChange = { onTurnOn() })
        )
    }
    AppInfoCard(
        icon = Icons.Default.Lock,
        heading = "End-to-end encrypted",
        message = "Back up your M-Pesa history with a passphrase only you know, so it survives a " +
            "lost or replaced phone. Your SMS messages are never uploaded."
    )
    if (backupFound) {
        SettingsSection(title = "Backup found for your account") {
            SettingsDetailItem(
                icon = Icons.Default.Restore,
                title = "Restore your M-Pesa history",
                subtitle = "Uses your backup passphrase",
                onClick = onRestore
            )
            SettingsRowDivider()
            SettingsDetailItem(
                icon = Icons.Default.LockReset,
                title = "Forgot the passphrase? Reset backup",
                danger = true,
                onClick = onReset
            )
        }
    }
}

@Composable
private fun EnabledContent(
    lastBackupAt: Long,
    lastError: String?,
    busy: Boolean,
    onBackupNow: () -> Unit,
    onChangePassphrase: () -> Unit,
    onRestore: () -> Unit,
    onReset: () -> Unit,
    onTurnOff: () -> Unit
) {
    SettingsSection(title = "Backup") {
        SettingsDetailItem(
            icon = Icons.Default.CloudDone,
            title = "Cloud backup",
            subtitle = if (lastBackupAt > 0) "On · last backup ${DateFormat.getDateTimeInstance().format(Date(lastBackupAt))}"
            else "On · waiting for the first backup",
            trailing = DetailTrailing.Toggle(checked = true, onCheckedChange = { onTurnOff() })
        )
        SettingsRowDivider()
        SettingsDetailItem(
            icon = Icons.Default.CloudUpload,
            title = "Back up now",
            enabled = !busy,
            onClick = onBackupNow
        )
        SettingsRowDivider()
        SettingsDetailItem(
            icon = Icons.Default.Key,
            title = "Change passphrase",
            enabled = !busy,
            onClick = onChangePassphrase
        )
    }
    lastError?.let {
        AppInfoCard(icon = Icons.Default.ErrorOutline, heading = "Last backup failed", message = it, tone = SheetTone.DANGER)
    }
    SettingsSection(title = "Restore") {
        SettingsDetailItem(
            icon = Icons.Default.Restore,
            title = "Restore from backup",
            onClick = onRestore
        )
        SettingsRowDivider()
        SettingsDetailItem(
            icon = Icons.Default.LockReset,
            title = "Forgot the passphrase? Reset backup",
            danger = true,
            onClick = onReset
        )
    }
}

@Composable
private fun ConsentForm(existingBackup: Boolean, busy: Boolean, onTurnOn: (CharArray) -> Unit) {
    var understood by remember { mutableStateOf(false) }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && pass != confirm
    val valid = understood && pass.length >= MIN_PASSPHRASE && pass == confirm

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Before you turn this on",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        AppInfoCard(
            icon = Icons.Default.CloudUpload,
            heading = "What's uploaded",
            message = "Receipt number, amount, date, type, transaction kind, merchant name, paybill " +
                "or till number, account number, transaction cost, and the categories you chose for " +
                "M-Pesa transactions."
        )
        AppInfoCard(
            icon = Icons.Default.VisibilityOff,
            heading = "What's never uploaded",
            message = "The SMS text, M-Pesa balances and phone numbers."
        )
        AppInfoCard(
            icon = Icons.Default.Lock,
            heading = "How it's protected",
            message = "It's encrypted on this phone with a backup passphrase that only you know. " +
                "FinTrack can't read it."
        )
        AppInfoCard(
            icon = Icons.Default.Warning,
            heading = "The trade-off",
            message = "If you forget the passphrase, the backup can't be restored. Nobody can recover it for you.",
            tone = SheetTone.DANGER
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (existingBackup) {
            Text(
                "A backup already exists for this account. Enter its passphrase to keep using it, " +
                    "or go back and reset the backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        AppPasswordField(pass, { pass = it }, "Backup passphrase (at least $MIN_PASSPHRASE characters)")
        AppPasswordField(confirm, { confirm = it }, "Repeat passphrase", isError = mismatch)
        if (mismatch) {
            Text(
                "The passphrases don't match.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        UnderstandRow(
            checked = understood,
            onCheckedChange = { understood = it },
            text = "I understand that if I forget the passphrase, my backup can't be restored."
        )
        Button(
            onClick = { onTurnOn(pass.toCharArray()) },
            enabled = valid && !busy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Turn on Cloud backup", fontWeight = FontWeight.Bold) }
    }
}

/** Checkbox row in a rounded, tappable container (whole row toggles). */
@Composable
private fun UnderstandRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Restore from the backup. Also used from the M-Pesa onboarding offer. */
@Composable
fun RestoreDialog(viewModel: MpesaBackupViewModel, onDismiss: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var pass by remember { mutableStateOf("") }
    val progress = ui.restoreProgress
    val result = ui.restoreResult

    when {
        progress != null -> {
            val (done, total) = progress
            AppActionSheet(
                title = "Restoring your M-Pesa history",
                icon = Icons.Default.Restore,
                heading = "Restoring... $done${if (total != null) " of $total" else ""}",
                onDismiss = {},
                dismissible = false,
                actions = emptyList()
            ) {
                if (total != null && total > 0) {
                    LinearProgressIndicator(
                        progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        result != null -> AppActionSheet(
            title = "Restore complete",
            icon = Icons.Default.CheckCircle,
            heading = "Restored ${result.transactions} transactions",
            onDismiss = { viewModel.clearMessages(); onDismiss() },
            actions = listOf(SheetAction("Done") { viewModel.clearMessages(); onDismiss() })
        )
        else -> AppActionSheet(
            title = "Restore your M-Pesa history",
            icon = Icons.Default.Restore,
            heading = "Enter your backup passphrase",
            message = "Use the passphrase you set when you turned on Cloud backup.",
            onDismiss = { viewModel.clearMessages(); viewModel.dismissOffer(); onDismiss() },
            actions = listOf(
                SheetAction("Not now", SheetActionKind.SECONDARY) {
                    viewModel.clearMessages(); viewModel.dismissOffer(); onDismiss()
                },
                SheetAction("Restore", enabled = pass.isNotEmpty() && !ui.busy) {
                    viewModel.restore(pass.toCharArray())
                }
            )
        ) {
            AppPasswordField(pass, { pass = it }, "Backup passphrase", isError = ui.error != null)
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChangePassphraseSheet(busy: Boolean, onChange: (CharArray, CharArray) -> Unit, onDismiss: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AppActionSheet(
        title = "Change passphrase",
        icon = Icons.Default.Key,
        heading = "Your backup stays the same",
        message = "Only the passphrase changes. Use the new one next time you restore.",
        onDismiss = onDismiss,
        actions = listOf(
            SheetAction("Cancel", SheetActionKind.SECONDARY, onClick = onDismiss),
            SheetAction(
                "Change",
                enabled = !busy && old.isNotEmpty() && new.length >= MIN_PASSPHRASE && new == confirm
            ) { onChange(old.toCharArray(), new.toCharArray()) }
        )
    ) {
        AppPasswordField(old, { old = it }, "Current passphrase")
        AppPasswordField(new, { new = it }, "New passphrase (at least $MIN_PASSPHRASE characters)")
        AppPasswordField(confirm, { confirm = it }, "Repeat new passphrase", isError = confirm.isNotEmpty() && new != confirm)
    }
}

@Composable
private fun ResetSheet(onReset: (CharArray) -> Unit, onDismiss: () -> Unit) {
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var understood by remember { mutableStateOf(false) }
    AppActionSheet(
        title = "Reset backup",
        icon = Icons.Default.LockReset,
        heading = "Your old backup will be deleted",
        message = "Without the old passphrase your existing backup can't be read, so it's deleted. " +
            "A new backup is then made from the M-Pesa data on this phone, protected by the new passphrase.",
        tone = SheetTone.DANGER,
        onDismiss = onDismiss,
        actions = listOf(
            SheetAction("Cancel", SheetActionKind.SECONDARY, onClick = onDismiss),
            SheetAction(
                "Reset backup",
                SheetActionKind.DANGER,
                enabled = understood && new.length >= MIN_PASSPHRASE && new == confirm
            ) { onReset(new.toCharArray()) }
        )
    ) {
        AppPasswordField(new, { new = it }, "New passphrase (at least $MIN_PASSPHRASE characters)")
        AppPasswordField(confirm, { confirm = it }, "Repeat new passphrase", isError = confirm.isNotEmpty() && new != confirm)
        UnderstandRow(checked = understood, onCheckedChange = { understood = it }, text = "Delete my old backup")
    }
}
